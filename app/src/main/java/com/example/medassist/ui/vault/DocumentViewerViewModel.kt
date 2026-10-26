package com.example.medassist.ui.vault

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.R
import com.example.medassist.data.local.PdfPageSource
import com.example.medassist.data.local.PdfPageSourceFactory
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.data.repository.VaultRepository
import com.example.medassist.data.repository.VaultResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

sealed interface ViewerState {
    data object Loading : ViewerState
    data class Error(@StringRes val messageRes: Int) : ViewerState
    data class Ready(val document: MedicalDocument, val pageCount: Int, val currentPage: Int = 0) : ViewerState
}

/**
 * Opens one vault PDF: downloads it (or reuses the cached copy), then hands pages to the screen.
 * [cacheFile] picks the private file for a document id.
 */
class DocumentViewerViewModel(
    private val documentId: String,
    private val repository: VaultRepository,
    private val cacheFile: (String) -> File,
    private val sourceFactory: PdfPageSourceFactory,
) : ViewModel() {

    private val _state = MutableStateFlow<ViewerState>(ViewerState.Loading)
    val state: StateFlow<ViewerState> = _state.asStateFlow()

    /** The open PDF. Only the screen reads it, to draw pages. */
    var source: PdfPageSource? = null
        private set

    init {
        load()
    }

    fun load() {
        _state.value = ViewerState.Loading
        closeSource()
        viewModelScope.launch {
            val document = when (val r = repository.getDocument(documentId)) {
                is VaultResult.Success -> r.value
                is VaultResult.Failure -> {
                    _state.value = ViewerState.Error(r.error.messageRes())
                    return@launch
                }
            }
            val file = cacheFile(document.id)
            if (!file.exists() || file.length() != document.sizeBytes) {
                // Download into a temporary name first, so a half-finished file is never opened.
                val partial = File(file.parentFile, file.name + ".part")
                when (val r = repository.downloadTo(document, partial)) {
                    is VaultResult.Failure -> {
                        partial.delete()
                        _state.value = ViewerState.Error(r.error.messageRes())
                        return@launch
                    }
                    is VaultResult.Success -> if (!partial.renameTo(file)) {
                        partial.delete()
                        _state.value = ViewerState.Error(R.string.vault_error_device_full)
                        return@launch
                    }
                }
            }
            val opened = try {
                sourceFactory.open(file)
            } catch (e: SecurityException) {
                _state.value = ViewerState.Error(R.string.vault_error_password_protected)
                return@launch
            } catch (e: IOException) {
                file.delete() // the cached copy is damaged; download again next time
                _state.value = ViewerState.Error(R.string.vault_error_corrupted)
                return@launch
            }
            source = opened
            _state.value = ViewerState.Ready(document, opened.pageCount)
        }
    }

    /** Called by the screen as the user scrolls or taps Next/Previous. */
    fun onPageVisible(index: Int) = _state.update {
        if (it is ViewerState.Ready) it.copy(currentPage = index.coerceIn(0, (it.pageCount - 1).coerceAtLeast(0))) else it
    }

    private fun closeSource() {
        source?.close()
        source = null
    }

    override fun onCleared() = closeSource()
}
