package com.example.medassist.ui.vault

import com.example.medassist.R
import com.example.medassist.data.local.PickedPdf
import com.example.medassist.data.local.PickedPdfResult
import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.repository.VaultError
import com.example.medassist.testutil.FakeVaultRepository
import com.example.medassist.testutil.FakeVaultRepository.Companion.doc
import com.example.medassist.testutil.MainDispatcherRule
import com.example.medassist.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VaultViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeVaultRepository()
    private val online = MutableStateFlow(true)
    private var pickResult: PickedPdfResult = PickedPdfResult.Ok(PickedPdf("report.pdf", "%PDF-1.4 test".toByteArray()))
    /** stateIn(WhileSubscribed) only runs while collected, as it is on screen. */
    private fun TestScope.vm() = VaultViewModel(repo, { pickResult }, online).also { v ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { v.list.collect {} }
    }

    @Test fun emptyVault_keepsCategoryFiltersAvailable() = runTest {
        val state = vm().list.value as UiState.Success
        assertTrue(state.data.documents.isEmpty())
        assertFalse(state.data.hasAnyDocuments)
    }

    @Test fun listError_showsMessage() = runTest {
        repo.listError = VaultError.PERMISSION_DENIED
        assertEquals(UiState.Error(R.string.vault_error_permission), vm().list.value)
    }

    @Test fun searchAndFilter_applyToList() = runTest {
        repo.docs.value = listOf(doc("a", "Blood", DocumentCategory.BLOOD_TESTS), doc("b", "Scan", DocumentCategory.IMAGING_REPORTS))
        val v = vm()
        v.onSearchChange("scan")
        assertEquals(listOf("b"), (v.list.value as UiState.Success).data.documents.map { it.id })
        v.onSearchChange("")
        v.onCategoryFilter(DocumentCategory.BLOOD_TESTS)
        assertEquals(listOf("a"), (v.list.value as UiState.Success).data.documents.map { it.id })
    }

    @Test fun categoryWithoutDocuments_returnsEmptyFilteredListInsteadOfEmptyState() = runTest {
        repo.docs.value = listOf(doc("rx", "Prescription", DocumentCategory.PRESCRIPTIONS))
        val v = vm()
        v.onCategoryFilter(DocumentCategory.BLOOD_TESTS)

        val result = (v.list.value as UiState.Success).data
        assertTrue(result.documents.isEmpty())
        assertTrue(result.hasAnyDocuments)
        assertEquals(DocumentCategory.BLOOD_TESTS, v.controls.value.category)
    }

    @Test fun invalidFile_showsMessage_andNoUploadSheet() = runTest {
        pickResult = PickedPdfResult.Invalid(VaultError.NOT_PDF)
        val v = vm()
        v.onFilePicked("content://x")
        assertNull(v.upload.value)
        assertEquals(R.string.vault_error_not_pdf, v.message.value?.messageRes)
    }

    @Test fun pickedFile_prefillsName_thenUploads() = runTest {
        val v = vm()
        v.onFilePicked("content://x")
        assertEquals("report", v.upload.value?.form?.name)
        v.onUploadCategoryChange(DocumentCategory.PRESCRIPTIONS)
        v.startUpload()
        assertNull(v.upload.value)
        assertEquals(R.string.vault_upload_done, v.message.value?.messageRes)
        assertEquals(DocumentCategory.PRESCRIPTIONS, repo.uploads.single().category)
        assertEquals(1, (v.list.value as UiState.Success).data.documents.size)
    }

    @Test fun emptyName_blocksUpload() = runTest {
        val v = vm()
        v.onFilePicked("content://x")
        v.onUploadNameChange("  ")
        v.startUpload()
        assertEquals(R.string.vault_error_doc_name_empty, v.upload.value?.form?.nameErrorRes)
        assertTrue(repo.uploads.isEmpty())
    }

    @Test fun failedUpload_canRetryWithSameId() = runTest {
        val v = vm()
        v.onFilePicked("content://x")
        repo.uploadError = VaultError.NETWORK
        v.startUpload()
        val failed = v.upload.value!!
        assertTrue(failed.failed)
        assertEquals(R.string.vault_error_network, failed.errorRes)
        repo.uploadError = null
        v.startUpload()
        assertNull(v.upload.value)
        assertEquals(repo.uploads[0].documentId, repo.uploads[1].documentId)
    }

    @Test fun cancelAfterFailure_discardsPieces() = runTest {
        val v = vm()
        v.onFilePicked("content://x")
        repo.uploadError = VaultError.NETWORK
        v.startUpload()
        v.cancelUpload()
        assertEquals(listOf("doc0"), repo.discarded)
    }

    @Test fun offline_blocksUpload() = runTest {
        online.value = false
        val v = vm()
        v.onFilePicked("content://x")
        v.startUpload()
        assertEquals(R.string.vault_error_offline, v.upload.value?.errorRes)
        assertTrue(repo.uploads.isEmpty())
    }

    @Test fun editRenamesAndRecategorizes() = runTest {
        repo.docs.value = listOf(doc("a", "Old"))
        val v = vm()
        v.startEdit(repo.docs.value.single())
        v.onEditNameChange("New name")
        v.onEditCategoryChange(DocumentCategory.MEDICAL_HISTORY)
        v.saveEdit()
        assertNull(v.edit.value)
        assertEquals("New name", repo.docs.value.single().name)
        assertEquals(DocumentCategory.MEDICAL_HISTORY, repo.docs.value.single().category)
    }

    @Test fun delete_confirmRemovesDocument() = runTest {
        repo.docs.value = listOf(doc("a", "Old"))
        val v = vm()
        v.requestDelete(repo.docs.value.single())
        v.confirmDelete()
        assertTrue(repo.docs.value.isEmpty())
        assertEquals(R.string.vault_deleted, v.message.value?.messageRes)
        val emptyState = (v.list.value as UiState.Success).data
        assertTrue(emptyState.documents.isEmpty())
        assertFalse(emptyState.hasAnyDocuments)
    }

    @Test fun deleteFailure_keepsDocument_andExplains() = runTest {
        repo.docs.value = listOf(doc("a", "Old"))
        repo.deleteError = VaultError.NETWORK
        val v = vm()
        v.requestDelete(repo.docs.value.single())
        v.confirmDelete()
        assertEquals(1, repo.docs.value.size)
        assertEquals(R.string.vault_error_network, v.message.value?.messageRes)
    }
}
