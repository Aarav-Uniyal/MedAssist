package com.example.medassist.data.repository

import android.util.Log
import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.domain.vault.PdfChunks
import com.example.medassist.domain.vault.PdfChunks.toHex
import com.example.medassist.domain.vault.VaultConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.security.MessageDigest

private const val TAG = "MedAssistVault"

/** How long one Firestore call may take before we report a network problem. */
private const val CALL_TIMEOUT_MS = 60_000L

/** Firestore allows 500 writes per batch; stay well below. */
private const val BATCH_LIMIT = 400

/**
 * Medical Vault on Cloud Firestore only, so it works on the free Spark plan
 * (Cloud Storage for Firebase needs the paid Blaze plan).
 *
 * Layout (all under the signed-in user, so security rules can check ownership by path):
 *
 *   users/{uid}/documents/{documentId}             metadata shown in the list
 *   users/{uid}/documentChunks/{documentId}_{i}    the PDF bytes, split into pieces of at most 900 KiB
 *
 * Pieces are written first and the metadata last, so a document only appears in the list once all
 * its pieces exist (the rules check this too). Pieces are stored as Firestore binary data (Blob),
 * not as Base64 text.
 */
class FirestoreVaultRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : VaultRepository {

    private fun documents(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("documents")

    private fun chunks(uid: String): CollectionReference =
        firestore.collection("users").document(uid).collection("documentChunks")

    override fun observeDocuments(): Flow<VaultResult<List<MedicalDocument>>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(VaultResult.Failure(VaultError.SESSION_EXPIRED))
            close()
            return@callbackFlow
        }
        val registration = documents(uid)
            .orderBy("uploadedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listening to documents failed", error)
                    trySend(VaultResult.Failure(error.toVaultError()))
                } else if (snapshot != null) {
                    trySend(VaultResult.Success(snapshot.documents.mapNotNull { it.toMedicalDocument() }))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getDocument(documentId: String): VaultResult<MedicalDocument> = withUser { uid ->
        val snapshot = call { documents(uid).document(documentId).get().await() }
        snapshot.toMedicalDocument()?.let { VaultResult.Success(it) } ?: VaultResult.Failure(VaultError.NOT_FOUND)
    }

    override fun newDocumentId(): String = firestore.collection("ids").document().id

    override suspend fun upload(request: UploadRequest, onProgress: (Float) -> Unit): VaultResult<Unit> =
        withUser { uid ->
            val pieces = withContext(Dispatchers.Default) { PdfChunks.split(request.bytes) }
            val sha256 = withContext(Dispatchers.Default) { PdfChunks.sha256Hex(request.bytes) }
            val steps = pieces.size + 1f
            onProgress(0f)
            pieces.forEachIndexed { index, piece ->
                call {
                    chunks(uid).document(PdfChunks.chunkId(request.documentId, index)).set(
                        mapOf(
                            "documentId" to request.documentId,
                            "index" to index,
                            "data" to Blob.fromBytes(piece),
                        ),
                    ).await()
                }
                onProgress((index + 1) / steps)
            }
            // Field names and types must match firestore.rules.
            call {
                documents(uid).document(request.documentId).set(
                    mapOf(
                        "ownerId" to uid,
                        "name" to request.name.trim(),
                        "category" to request.category.key,
                        "notes" to request.notes.trim(),
                        "sizeBytes" to request.bytes.size.toLong(),
                        "mimeType" to VaultConfig.MIME_PDF,
                        "chunkCount" to pieces.size,
                        "sha256" to sha256,
                        "storagePath" to "users/$uid/documentChunks",
                        "uploadedAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
            onProgress(1f)
            VaultResult.Success(Unit)
        }

    override suspend fun discardUpload(documentId: String, sizeBytes: Long) {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            deleteInBatches(PdfChunks.chunkIds(documentId, PdfChunks.chunkCount(sizeBytes)).map { chunks(uid).document(it) })
        }.onFailure { if (it is CancellationException) throw it else Log.w(TAG, "Discarding upload failed", it) }
    }

    override suspend fun updateDetails(
        documentId: String,
        name: String,
        category: DocumentCategory,
        notes: String,
    ): VaultResult<Unit> = withUser { uid ->
        call {
            documents(uid).document(documentId).update(
                mapOf("name" to name.trim(), "category" to category.key, "notes" to notes.trim()),
            ).await()
        }
        VaultResult.Success(Unit)
    }

    override suspend fun delete(document: MedicalDocument): VaultResult<Unit> = withUser { uid ->
        // One batch: the metadata and all pieces disappear together, or nothing changes.
        val refs = listOf(documents(uid).document(document.id)) +
            PdfChunks.chunkIds(document.id, document.chunkCount).map { chunks(uid).document(it) }
        deleteInBatches(refs)
        VaultResult.Success(Unit)
    }

    override suspend fun downloadTo(document: MedicalDocument, target: File): VaultResult<Unit> = withUser { uid ->
        val digest = MessageDigest.getInstance("SHA-256")
        var written = 0L
        target.parentFile?.mkdirs()
        // Pieces are fetched one at a time so a 10 MB file never sits in memory twice.
        target.outputStream().buffered().use { out ->
            for (id in PdfChunks.chunkIds(document.id, document.chunkCount)) {
                val bytes = call { chunks(uid).document(id).get().await() }.getBlob("data")?.toBytes()
                    ?: return@withUser VaultResult.Failure(VaultError.CORRUPTED)
                withContext(Dispatchers.IO) { out.write(bytes) }
                digest.update(bytes)
                written += bytes.size
            }
        }
        if (written != document.sizeBytes || digest.digest().toHex() != document.sha256) {
            target.delete()
            return@withUser VaultResult.Failure(VaultError.CORRUPTED)
        }
        VaultResult.Success(Unit)
    }

    override suspend fun deleteAllUserData(uid: String) {
        val metadata = call { documents(uid).get().await() }
        for (doc in metadata.documents) {
            val count = doc.getLong("chunkCount")?.toInt() ?: 0
            deleteInBatches(listOf(doc.reference) + PdfChunks.chunkIds(doc.id, count).map { chunks(uid).document(it) })
        }
        // Sweep up pieces of interrupted uploads that never got metadata.
        while (true) {
            val leftovers = call { chunks(uid).limit(20).get().await() }
            if (leftovers.isEmpty) break
            deleteInBatches(leftovers.documents.map { it.reference })
        }
    }

    private suspend fun deleteInBatches(refs: List<DocumentReference>) {
        refs.chunked(BATCH_LIMIT).forEach { group ->
            call {
                firestore.batch().apply { group.forEach { delete(it) } }.commit().await()
            }
        }
    }

    /** Runs a Firestore call with a timeout. Firestore would otherwise wait forever while offline. */
    private suspend fun <T> call(block: suspend () -> T): T = withTimeout(CALL_TIMEOUT_MS) { block() }

    /** Runs [block] for the signed-in user, turning any failure into a [VaultResult.Failure]. */
    private suspend fun <T> withUser(block: suspend (uid: String) -> VaultResult<T>): VaultResult<T> {
        val uid = auth.currentUser?.uid ?: return VaultResult.Failure(VaultError.SESSION_EXPIRED)
        return try {
            block(uid)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Vault operation timed out", e)
            VaultResult.Failure(VaultError.NETWORK)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Vault operation failed", e)
            VaultResult.Failure(e.toVaultError())
        }
    }

    private fun DocumentSnapshot.toMedicalDocument(): MedicalDocument? {
        if (!exists()) return null
        return MedicalDocument(
            id = id,
            name = getString("name") ?: return null,
            category = DocumentCategory.fromKey(getString("category")),
            notes = getString("notes").orEmpty(),
            sizeBytes = getLong("sizeBytes") ?: return null,
            // Right after an upload the server time is not known yet; use the phone's estimate.
            uploadedAtMillis = getTimestamp("uploadedAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: 0L,
            chunkCount = getLong("chunkCount")?.toInt() ?: return null,
            sha256 = getString("sha256").orEmpty(),
        )
    }
}
