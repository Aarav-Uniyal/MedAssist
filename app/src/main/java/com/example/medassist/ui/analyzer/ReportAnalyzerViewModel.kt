package com.example.medassist.ui.analyzer

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.R
import com.example.medassist.data.local.DocumentCache
import com.example.medassist.data.local.PdfTextExtractionResult
import com.example.medassist.data.local.PdfTextExtractor
import com.example.medassist.data.model.MedicalDocument
import com.example.medassist.data.model.ReportAnalysis
import com.example.medassist.data.repository.AssistantError
import com.example.medassist.data.repository.ReportAnalysisResult
import com.example.medassist.data.repository.ReportAnalyzerRepository
import com.example.medassist.data.repository.VaultRepository
import com.example.medassist.data.repository.VaultResult
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.vault.messageRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

sealed interface AnalyzerWorkState {
    data object Idle : AnalyzerWorkState
    data class Working(val stage: AnalyzerStage) : AnalyzerWorkState
    data class Completed(val result: AnalyzerDisplayResult) : AnalyzerWorkState
    data class Failed(@StringRes val messageRes: Int, val retryable: Boolean) : AnalyzerWorkState
}

enum class AnalyzerStage { DOWNLOADING, EXTRACTING, ANALYZING }

data class AnalyzerDisplayResult(
    val analysis: ReportAnalysis,
    val totalPages: Int,
    val pagesRead: Int,
    val textTruncated: Boolean,
)

data class AnalyzerUiState(
    val documents: UiState<List<MedicalDocument>> = UiState.Loading,
    val consentDocument: MedicalDocument? = null,
    val selectedDocument: MedicalDocument? = null,
    val work: AnalyzerWorkState = AnalyzerWorkState.Idle,
)

/** Coordinates owner-scoped PDF retrieval, on-device extraction, explicit consent and AI analysis. */
class ReportAnalyzerViewModel(
    private val uid: String,
    private val vaultRepository: VaultRepository,
    private val documentCache: DocumentCache,
    private val pdfTextExtractor: PdfTextExtractor,
    private val analyzerRepository: ReportAnalyzerRepository,
    isOnline: Flow<Boolean>,
    private val language: () -> AppLanguage,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyzerUiState())
    val state: StateFlow<AnalyzerUiState> = _state.asStateFlow()
    val isOnline: StateFlow<Boolean> = isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var listenJob: Job? = null
    private var analysisJob: Job? = null

    init {
        reloadDocuments()
    }

    fun reloadDocuments() {
        listenJob?.cancel()
        _state.update { it.copy(documents = UiState.Loading) }
        listenJob = viewModelScope.launch {
            vaultRepository.observeDocuments().collect { result ->
                _state.update { current ->
                    current.copy(
                        documents = when (result) {
                            is VaultResult.Success -> if (result.value.isEmpty()) UiState.Empty else UiState.Success(result.value)
                            is VaultResult.Failure -> UiState.Error(result.error.messageRes())
                        },
                    )
                }
            }
        }
    }

    /** Selecting a file always opens a fresh consent dialog before its text is sent to Gemini. */
    fun requestAnalysis(document: MedicalDocument) {
        if (_state.value.work is AnalyzerWorkState.Working) return
        _state.update {
            it.copy(
                selectedDocument = document,
                consentDocument = document,
                work = AnalyzerWorkState.Idle,
            )
        }
    }

    fun cancelConsent() {
        _state.update { it.copy(consentDocument = null, selectedDocument = null) }
    }

    fun confirmConsent() {
        val document = _state.value.consentDocument ?: return
        _state.update { it.copy(consentDocument = null, selectedDocument = document) }
        analyze(document)
    }

    /** Retries the last chosen report after a transient download or AI error. */
    fun retry() {
        val document = _state.value.selectedDocument ?: return
        if (_state.value.work is AnalyzerWorkState.Working) return
        analyze(document)
    }

    private fun analyze(selected: MedicalDocument) {
        if (_state.value.work is AnalyzerWorkState.Working) return
        analysisJob?.cancel()

        if (!isOnline.value) {
            _state.update { it.copy(work = AnalyzerWorkState.Failed(R.string.analyzer_error_offline, retryable = true)) }
            return
        }

        _state.update { it.copy(work = AnalyzerWorkState.Working(AnalyzerStage.DOWNLOADING)) }
        analysisJob = viewModelScope.launch {
            try {
                // Re-fetch metadata from the signed-in user's vault instead of trusting a stale list item.
                val document = when (val fetched = vaultRepository.getDocument(selected.id)) {
                    is VaultResult.Success -> fetched.value
                    is VaultResult.Failure -> {
                        fail(fetched.error.messageRes(), retryable = fetched.error in RETRYABLE_VAULT_ERRORS)
                        return@launch
                    }
                }
                val file = documentCache.fileFor(uid, document.id)
                if (!cacheMatches(file, document)) {
                    val partial = File(file.parentFile, file.name + ".analyze.part")
                    partial.delete()
                    when (val downloaded = vaultRepository.downloadTo(document, partial)) {
                        is VaultResult.Failure -> {
                            partial.delete()
                            fail(downloaded.error.messageRes(), retryable = downloaded.error in RETRYABLE_VAULT_ERRORS)
                            return@launch
                        }
                        is VaultResult.Success -> {
                            file.delete()
                            if (!partial.renameTo(file)) {
                                partial.delete()
                                fail(R.string.vault_error_device_full, retryable = true)
                                return@launch
                            }
                        }
                    }
                }

                _state.update { it.copy(work = AnalyzerWorkState.Working(AnalyzerStage.EXTRACTING)) }
                when (val extracted = pdfTextExtractor.extract(file)) {
                    is PdfTextExtractionResult.NoReadableText -> {
                        fail(R.string.analyzer_error_scanned, retryable = false)
                        return@launch
                    }
                    PdfTextExtractionResult.PasswordProtected -> {
                        fail(R.string.analyzer_error_pdf_locked, retryable = false)
                        return@launch
                    }
                    PdfTextExtractionResult.Unreadable -> {
                        fail(R.string.analyzer_error_pdf_unreadable, retryable = false)
                        return@launch
                    }
                    is PdfTextExtractionResult.Success -> {
                        if (!isOnline.value) {
                            fail(R.string.analyzer_error_offline, retryable = true)
                            return@launch
                        }
                        _state.update { it.copy(work = AnalyzerWorkState.Working(AnalyzerStage.ANALYZING)) }
                        when (val result = analyzerRepository.analyze(extracted.text, language())) {
                            is ReportAnalysisResult.Failure -> fail(
                                messageRes = result.error.messageRes(),
                                retryable = result.error in RETRYABLE_AI_ERRORS,
                            )
                            is ReportAnalysisResult.Success -> _state.update {
                                it.copy(
                                    work = AnalyzerWorkState.Completed(
                                        AnalyzerDisplayResult(
                                            analysis = result.analysis,
                                            totalPages = extracted.totalPages,
                                            pagesRead = extracted.pagesRead,
                                            textTruncated = extracted.truncated,
                                        ),
                                    ),
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The exception may contain service data; show a generic message and don't log report text.
                fail(R.string.common_error_generic, retryable = true)
            }
        }
    }

    private fun fail(@StringRes messageRes: Int, retryable: Boolean) {
        _state.update { it.copy(work = AnalyzerWorkState.Failed(messageRes, retryable)) }
    }

    /** Reuse a private cached copy only when both size and SHA-256 match the owner-scoped metadata. */
    private suspend fun cacheMatches(file: File, document: MedicalDocument): Boolean = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() != document.sizeBytes || document.sha256.isBlank()) return@withContext false
        runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).buffered().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == document.sha256
        }.getOrDefault(false)
    }

    override fun onCleared() {
        analysisJob?.cancel()
        listenJob?.cancel()
        super.onCleared()
    }

    private fun AssistantError.messageRes(): Int = when (this) {
        AssistantError.NETWORK -> R.string.chat_error_network
        AssistantError.TIMEOUT -> R.string.chat_error_timeout
        AssistantError.QUOTA -> R.string.chat_error_quota
        AssistantError.BLOCKED -> R.string.chat_error_blocked
        AssistantError.NOT_SET_UP -> R.string.chat_error_not_set_up
        AssistantError.APP_CHECK -> R.string.chat_error_app_check
        AssistantError.REGION -> R.string.chat_error_region
        AssistantError.SERVER -> R.string.chat_error_server
        AssistantError.EMPTY_REPLY -> R.string.chat_error_empty
        AssistantError.UNKNOWN -> R.string.chat_error_unknown
    }

    private companion object {
        val RETRYABLE_VAULT_ERRORS = setOf(
            com.example.medassist.data.repository.VaultError.NETWORK,
            com.example.medassist.data.repository.VaultError.QUOTA_EXCEEDED,
            com.example.medassist.data.repository.VaultError.UNKNOWN,
        )
        val RETRYABLE_AI_ERRORS = setOf(
            AssistantError.NETWORK,
            AssistantError.TIMEOUT,
            AssistantError.QUOTA,
            AssistantError.SERVER,
            AssistantError.EMPTY_REPLY,
            AssistantError.UNKNOWN,
        )
    }
}
