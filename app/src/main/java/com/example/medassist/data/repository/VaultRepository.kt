package com.example.medassist.data.repository

import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.MedicalDocument
import kotlinx.coroutines.flow.Flow
import java.io.File

/** Everything that can go wrong in the Medical Vault, in app terms. Each has a localized message. */
enum class VaultError {
    NETWORK,
    SESSION_EXPIRED,
    PERMISSION_DENIED,
    NOT_FOUND,
    QUOTA_EXCEEDED,
    FILE_EMPTY,
    FILE_TOO_LARGE,
    NOT_PDF,
    PASSWORD_PROTECTED,
    CORRUPTED,
    UNREADABLE,
    DEVICE_STORAGE_FULL,
    UNKNOWN,
}

sealed interface VaultResult<out T> {
    data class Success<T>(val value: T) : VaultResult<T>
    data class Failure(val error: VaultError) : VaultResult<Nothing>
}

/** What the user chose to upload, after the file was checked. */
data class UploadRequest(
    val documentId: String,
    val bytes: ByteArray,
    val name: String,
    val category: DocumentCategory,
    val notes: String,
)

/**
 * The only way the app talks to the vault's storage. An interface so ViewModel tests can use a fake.
 * Every operation works on the signed-in user's own documents; there is no way to name another user.
 */
interface VaultRepository {
    /** The user's documents, newest first, updated live. */
    fun observeDocuments(): Flow<VaultResult<List<MedicalDocument>>>

    suspend fun getDocument(documentId: String): VaultResult<MedicalDocument>

    /** A fresh random id for a new upload. Re-using it on retry overwrites the same pieces. */
    fun newDocumentId(): String

    /** Uploads the PDF. [onProgress] goes from 0 to 1. */
    suspend fun upload(request: UploadRequest, onProgress: (Float) -> Unit): VaultResult<Unit>

    /** Best-effort clean-up of pieces left behind by an upload the user gave up on. */
    suspend fun discardUpload(documentId: String, sizeBytes: Long)

    suspend fun updateDetails(
        documentId: String,
        name: String,
        category: DocumentCategory,
        notes: String,
    ): VaultResult<Unit>

    suspend fun delete(document: MedicalDocument): VaultResult<Unit>

    /** Downloads the PDF into [target] and checks it is complete and unchanged. */
    suspend fun downloadTo(document: MedicalDocument, target: File): VaultResult<Unit>

    /** Deletes every vault document of [uid]. Throws on failure. Used by account deletion. */
    suspend fun deleteAllUserData(uid: String)
}
