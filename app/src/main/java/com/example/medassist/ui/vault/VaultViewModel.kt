package com.example.medassist.ui.vault

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.R
import com.example.medassist.data.local.PickedPdfReader
import com.example.medassist.data.local.PickedPdfResult
import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.DocumentSort
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.data.repository.UploadRequest
import com.example.medassist.data.repository.VaultRepository
import com.example.medassist.data.repository.VaultResult
import com.example.medassist.domain.validation.DocumentValidator
import com.example.medassist.domain.vault.DocumentQuery
import com.example.medassist.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The name/category/notes form, used both before an upload and when editing a document. */
data class DocumentForm(
    val name: String = "",
    val category: DocumentCategory = DocumentCategory.OTHER,
    val notes: String = "",
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val notesErrorRes: Int? = null,
)

/** The upload sheet. [bytes] stays in memory so "Retry" does not need the file picker again. */
data class UploadDraft(
    val documentId: String,
    val bytes: ByteArray,
    val form: DocumentForm,
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    @StringRes val errorRes: Int? = null,
) {
    val sizeBytes: Long get() = bytes.size.toLong()
    val failed: Boolean get() = errorRes != null && !isUploading
}

data class EditDraft(
    val document: MedicalDocument,
    val form: DocumentForm,
    val isSaving: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

data class VaultControls(
    val search: String = "",
    val category: DocumentCategory? = null,
    val sort: DocumentSort = DocumentSort.NEWEST,
)

/** What the vault list shows. [hasAnyDocuments] tells "no documents yet" apart from "no search results". */
data class VaultList(val documents: List<MedicalDocument>, val hasAnyDocuments: Boolean)

/** Short-lived message for a snackbar. */
data class VaultMessage(@StringRes val messageRes: Int, val id: Long = System.nanoTime())

class VaultViewModel(
    private val repository: VaultRepository,
    private val pdfReader: PickedPdfReader,
    isOnline: Flow<Boolean> = flowOf(true),
) : ViewModel() {

    // Firestore quietly queues writes made offline and sends them later. For a 10 MB upload or a
    // delete the user expects to happen now, so those actions wait for a connection instead.
    private val online: StateFlow<Boolean> = isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _controls = MutableStateFlow(VaultControls())
    val controls: StateFlow<VaultControls> = _controls.asStateFlow()

    private val _upload = MutableStateFlow<UploadDraft?>(null)
    val upload: StateFlow<UploadDraft?> = _upload.asStateFlow()

    private val _edit = MutableStateFlow<EditDraft?>(null)
    val edit: StateFlow<EditDraft?> = _edit.asStateFlow()

    private val _pendingDelete = MutableStateFlow<MedicalDocument?>(null)
    val pendingDelete: StateFlow<MedicalDocument?> = _pendingDelete.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _isReadingFile = MutableStateFlow(false)
    val isReadingFile: StateFlow<Boolean> = _isReadingFile.asStateFlow()

    private val _message = MutableStateFlow<VaultMessage?>(null)
    val message: StateFlow<VaultMessage?> = _message.asStateFlow()

    private val rawDocuments = MutableStateFlow<UiState<List<MedicalDocument>>>(UiState.Loading)
    private var listenJob: Job? = null
    private var uploadJob: Job? = null

    val list: StateFlow<UiState<VaultList>> = combine(rawDocuments, _controls) { raw, c ->
        when (raw) {
            is UiState.Success -> UiState.Success(
                VaultList(
                    documents = DocumentQuery.apply(raw.data, c.search, c.category, c.sort),
                    hasAnyDocuments = raw.data.isNotEmpty(),
                ),
            )
            // Keep the search box and category chips visible when the vault is empty. This lets
            // users select any category and see a useful category-specific empty state.
            UiState.Empty -> UiState.Success(VaultList(documents = emptyList(), hasAnyDocuments = false))
            UiState.Loading -> UiState.Loading
            is UiState.Error -> raw
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    init {
        listen()
    }

    /** (Re)starts the live document list. Also used by the "Retry" button. */
    fun listen() {
        listenJob?.cancel()
        rawDocuments.value = UiState.Loading
        listenJob = viewModelScope.launch {
            repository.observeDocuments().collect { result ->
                rawDocuments.value = when (result) {
                    is VaultResult.Success -> if (result.value.isEmpty()) UiState.Empty else UiState.Success(result.value)
                    is VaultResult.Failure -> UiState.Error(result.error.messageRes())
                }
            }
        }
    }

    fun onSearchChange(value: String) = _controls.update { it.copy(search = value) }
    fun onCategoryFilter(category: DocumentCategory?) = _controls.update { it.copy(category = category) }
    fun onSortChange(sort: DocumentSort) = _controls.update { it.copy(sort = sort) }

    fun consumeMessage() {
        _message.value = null
    }

    // ---- Upload ----

    /** Called with the URI the system file picker returned. */
    fun onFilePicked(uri: String, fileName: String? = null) {
        if (_isReadingFile.value || _upload.value?.isUploading == true) return
        _isReadingFile.value = true
        viewModelScope.launch {
            when (val result = pdfReader.read(uri)) {
                is PickedPdfResult.Invalid -> _message.value = VaultMessage(result.error.messageRes())
                is PickedPdfResult.Ok -> _upload.value = UploadDraft(
                    documentId = repository.newDocumentId(),
                    bytes = result.pdf.bytes,
                    form = DocumentForm(name = DocumentValidator.suggestedName(result.pdf.fileName ?: fileName)),
                )
            }
            _isReadingFile.value = false
        }
    }

    fun onUploadNameChange(value: String) = updateUploadForm { it.copy(name = value, nameErrorRes = null) }
    fun onUploadCategoryChange(value: DocumentCategory) = updateUploadForm { it.copy(category = value) }
    fun onUploadNotesChange(value: String) = updateUploadForm { it.copy(notes = value, notesErrorRes = null) }

    private fun updateUploadForm(change: (DocumentForm) -> DocumentForm) = _upload.update { draft ->
        if (draft == null || draft.isUploading) draft else draft.copy(form = change(draft.form), errorRes = null)
    }

    /** Starts the upload, or retries it after a failure (same id, so pieces are overwritten, not duplicated). */
    fun startUpload() {
        val draft = _upload.value ?: return
        if (draft.isUploading) return
        if (!online.value) {
            _upload.value = draft.copy(errorRes = R.string.vault_error_offline)
            return
        }
        val checked = draft.form.validated()
        if (checked.nameErrorRes != null || checked.notesErrorRes != null) {
            _upload.value = draft.copy(form = checked)
            return
        }
        _upload.value = draft.copy(form = checked, isUploading = true, progress = 0f, errorRes = null)
        uploadJob = viewModelScope.launch {
            val request = UploadRequest(
                documentId = draft.documentId,
                bytes = draft.bytes,
                name = checked.name,
                category = checked.category,
                notes = checked.notes,
            )
            val result = repository.upload(request) { p -> _upload.update { it?.copy(progress = p) } }
            when (result) {
                is VaultResult.Success -> {
                    _upload.value = null
                    _message.value = VaultMessage(R.string.vault_upload_done)
                }
                is VaultResult.Failure -> _upload.update {
                    it?.copy(isUploading = false, errorRes = result.error.messageRes())
                }
            }
        }
    }

    /** Closes the upload sheet. A failed or half-finished upload is cleaned up in the background. */
    fun cancelUpload() {
        val draft = _upload.value ?: return
        uploadJob?.cancel()
        _upload.value = null
        if (draft.isUploading || draft.failed) {
            viewModelScope.launch { repository.discardUpload(draft.documentId, draft.sizeBytes) }
        }
    }

    // ---- Edit (rename, category, notes) ----

    fun startEdit(document: MedicalDocument) {
        _edit.value = EditDraft(document, DocumentForm(document.name, document.category, document.notes))
    }

    fun onEditNameChange(value: String) = updateEditForm { it.copy(name = value, nameErrorRes = null) }
    fun onEditCategoryChange(value: DocumentCategory) = updateEditForm { it.copy(category = value) }
    fun onEditNotesChange(value: String) = updateEditForm { it.copy(notes = value, notesErrorRes = null) }

    private fun updateEditForm(change: (DocumentForm) -> DocumentForm) = _edit.update { draft ->
        if (draft == null || draft.isSaving) draft else draft.copy(form = change(draft.form), errorRes = null)
    }

    fun saveEdit() {
        val draft = _edit.value ?: return
        if (draft.isSaving) return
        if (!online.value) {
            _edit.value = draft.copy(errorRes = R.string.vault_error_offline)
            return
        }
        val checked = draft.form.validated()
        if (checked.nameErrorRes != null || checked.notesErrorRes != null) {
            _edit.value = draft.copy(form = checked)
            return
        }
        _edit.value = draft.copy(form = checked, isSaving = true, errorRes = null)
        viewModelScope.launch {
            when (val r = repository.updateDetails(draft.document.id, checked.name, checked.category, checked.notes)) {
                is VaultResult.Success -> {
                    _edit.value = null
                    _message.value = VaultMessage(R.string.vault_saved)
                }
                is VaultResult.Failure -> _edit.update { it?.copy(isSaving = false, errorRes = r.error.messageRes()) }
            }
        }
    }

    fun cancelEdit() {
        if (_edit.value?.isSaving == true) return
        _edit.value = null
    }

    // ---- Delete ----

    fun requestDelete(document: MedicalDocument) {
        _pendingDelete.value = document
    }

    fun cancelDelete() {
        if (!_isDeleting.value) _pendingDelete.value = null
    }

    fun confirmDelete() {
        val doc = _pendingDelete.value ?: return
        if (_isDeleting.value) return
        if (!online.value) {
            _pendingDelete.value = null
            _message.value = VaultMessage(R.string.vault_error_offline)
            return
        }
        _isDeleting.value = true
        viewModelScope.launch {
            val r = repository.delete(doc)
            _isDeleting.value = false
            _pendingDelete.value = null
            _message.value = VaultMessage(
                when (r) {
                    is VaultResult.Success -> R.string.vault_deleted
                    is VaultResult.Failure -> r.error.messageRes()
                },
            )
        }
    }

    private fun DocumentForm.validated(): DocumentForm = copy(
        name = name.trim(),
        notes = notes.trim(),
        nameErrorRes = DocumentValidator.validateName(name)?.messageRes(),
        notesErrorRes = DocumentValidator.validateNotes(notes)?.messageRes(),
    )
}
