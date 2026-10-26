package com.example.medassist.ui.chatbot

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.R
import com.example.medassist.data.local.ChatHistoryStore
import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.model.ChatRole
import com.example.medassist.data.repository.AssistantError
import com.example.medassist.data.repository.AssistantRepository
import com.example.medassist.data.repository.AssistantResult
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.domain.assistant.AssistantConfig
import com.example.medassist.domain.assistant.EmergencyDetector
import com.example.medassist.domain.assistant.RateLimiter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @StringRes val inputErrorRes: Int? = null,
    val showEmergency: Boolean = false,
    val historyEnabled: Boolean = false,
    val confirmNewConversation: Boolean = false,
)

class ChatViewModel(
    private val uid: String,
    private val repository: AssistantRepository,
    private val historyStore: ChatHistoryStore,
    isOnline: Flow<Boolean>,
    private val language: () -> AppLanguage,
    private val clock: () -> Long = System::currentTimeMillis,
    private val rateLimiter: RateLimiter = RateLimiter(clock = clock),
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState(historyEnabled = historyStore.isEnabled(uid)))
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    val isOnline: StateFlow<Boolean> = isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        if (_state.value.historyEnabled) {
            viewModelScope.launch {
                val saved = historyStore.load(uid)
                _state.update { if (it.messages.isEmpty()) it.copy(messages = saved) else it }
            }
        }
    }

    fun onInputChange(text: String) {
        _state.update {
            it.copy(
                input = text,
                inputErrorRes = if (text.length > AssistantConfig.MAX_MESSAGE_CHARS) R.string.chat_error_too_long else null,
            )
        }
    }

    fun send() {
        val text = _state.value.input.trim()
        if (text.isEmpty() || _state.value.sending) return
        if (text.length > AssistantConfig.MAX_MESSAGE_CHARS) {
            _state.update { it.copy(inputErrorRes = R.string.chat_error_too_long) }
            return
        }
        ask(text, clearInput = true)
    }

    fun useSuggestion(text: String) {
        if (!_state.value.sending) ask(text, clearInput = false)
    }

    /** Sends a question that failed again, moving it to the end of the chat. */
    fun retry(messageId: String) {
        val failed = _state.value.messages.firstOrNull { it.id == messageId && it.failed } ?: return
        if (_state.value.sending) return
        _state.update { s -> s.copy(messages = s.messages.filterNot { it.id == messageId }) }
        ask(failed.text, clearInput = false)
    }

    private fun ask(text: String, clearInput: Boolean) {
        viewModelScope.launch {
            if (!isOnline.first()) {
                _state.update { it.copy(errorRes = R.string.chat_error_offline) }
                return@launch
            }
            if (!rateLimiter.tryAcquire()) {
                _state.update { it.copy(errorRes = R.string.chat_error_rate_limited) }
                return@launch
            }
            val question = ChatMessage(newId(), ChatRole.USER, text, clock())
            _state.update {
                it.copy(
                    messages = it.messages + question,
                    input = if (clearInput) "" else it.input,
                    inputErrorRes = null,
                    errorRes = null,
                    sending = true,
                    showEmergency = it.showEmergency || EmergencyDetector.looksUrgent(text),
                )
            }
            val result = repository.reply(_state.value.messages, language())
            _state.update { s ->
                when (result) {
                    is AssistantResult.Reply -> s.copy(
                        sending = false,
                        messages = s.messages + ChatMessage(newId(), ChatRole.ASSISTANT, result.text, clock()),
                    )
                    is AssistantResult.Failure -> s.copy(
                        sending = false,
                        errorRes = errorText(result.error),
                        messages = s.messages.map { if (it.id == question.id) it.copy(failed = true) else it },
                    )
                }
            }
            persist()
        }
    }

    fun requestNewConversation() {
        if (_state.value.messages.isNotEmpty()) _state.update { it.copy(confirmNewConversation = true) }
    }

    fun dismissNewConversation() = _state.update { it.copy(confirmNewConversation = false) }

    /** Starts over and deletes any saved copy of this chat. */
    fun confirmNewConversation() {
        _state.update {
            it.copy(messages = emptyList(), confirmNewConversation = false, errorRes = null, showEmergency = false)
        }
        viewModelScope.launch { historyStore.clear(uid) }
    }

    fun setHistoryEnabled(enabled: Boolean) {
        historyStore.setEnabled(uid, enabled)
        _state.update { it.copy(historyEnabled = enabled) }
        viewModelScope.launch {
            if (enabled) historyStore.save(uid, _state.value.messages) else historyStore.clear(uid)
        }
    }

    fun dismissEmergency() = _state.update { it.copy(showEmergency = false) }

    fun dismissError() = _state.update { it.copy(errorRes = null) }

    private suspend fun persist() {
        if (_state.value.historyEnabled) historyStore.save(uid, _state.value.messages)
    }

    companion object {
        @StringRes
        fun errorText(error: AssistantError): Int = when (error) {
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
    }
}
