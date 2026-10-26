package com.example.medassist.testutil

import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.data.repository.UploadRequest
import com.example.medassist.data.repository.VaultError
import com.example.medassist.data.repository.VaultRepository
import com.example.medassist.data.repository.VaultResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

/** In-memory [VaultRepository] for ViewModel tests. Set the *Error fields to make the next call fail. */
class FakeVaultRepository(initial: List<MedicalDocument> = emptyList()) : VaultRepository {
    val docs = MutableStateFlow(initial)
    var listError: VaultError? = null
    var uploadError: VaultError? = null
    var updateError: VaultError? = null
    var deleteError: VaultError? = null
    val uploads = mutableListOf<UploadRequest>()
    val discarded = mutableListOf<String>()
    private var nextId = 0

    override fun observeDocuments(): Flow<VaultResult<List<MedicalDocument>>> = docs.map { list ->
        listError?.let { VaultResult.Failure(it) } ?: VaultResult.Success(list)
    }

    override suspend fun getDocument(documentId: String): VaultResult<MedicalDocument> =
        docs.value.firstOrNull { it.id == documentId }?.let { VaultResult.Success(it) }
            ?: VaultResult.Failure(VaultError.NOT_FOUND)

    override fun newDocumentId(): String = "doc${nextId++}"

    override suspend fun upload(request: UploadRequest, onProgress: (Float) -> Unit): VaultResult<Unit> {
        uploads += request
        onProgress(0.5f)
        uploadError?.let { return VaultResult.Failure(it) }
        onProgress(1f)
        docs.value = docs.value + doc(request.documentId, request.name, request.category, sizeBytes = request.bytes.size.toLong())
        return VaultResult.Success(Unit)
    }

    override suspend fun discardUpload(documentId: String, sizeBytes: Long) {
        discarded += documentId
    }

    override suspend fun updateDetails(documentId: String, name: String, category: DocumentCategory, notes: String): VaultResult<Unit> {
        updateError?.let { return VaultResult.Failure(it) }
        docs.value = docs.value.map { if (it.id == documentId) it.copy(name = name, category = category, notes = notes) else it }
        return VaultResult.Success(Unit)
    }

    override suspend fun delete(document: MedicalDocument): VaultResult<Unit> {
        deleteError?.let { return VaultResult.Failure(it) }
        docs.value = docs.value.filterNot { it.id == document.id }
        return VaultResult.Success(Unit)
    }

    override suspend fun downloadTo(document: MedicalDocument, target: File): VaultResult<Unit> = VaultResult.Success(Unit)

    override suspend fun deleteAllUserData(uid: String) {
        docs.value = emptyList()
    }

    companion object {
        fun doc(
            id: String,
            name: String,
            category: DocumentCategory = DocumentCategory.OTHER,
            uploadedAt: Long = 0L,
            sizeBytes: Long = 1000L,
            notes: String = "",
        ) = MedicalDocument(id, name, category, notes, sizeBytes, uploadedAt, chunkCount = 1, sha256 = "")
    }
}
