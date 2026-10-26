package com.example.medassist.data.repository

import com.example.medassist.data.model.ChatMessage
import com.example.medassist.domain.assistant.AppLanguage

enum class AssistantError {
    NETWORK, TIMEOUT, QUOTA, BLOCKED, NOT_SET_UP, APP_CHECK, REGION, SERVER, EMPTY_REPLY, UNKNOWN
}

sealed interface AssistantResult {
    data class Reply(val text: String) : AssistantResult
    data class Failure(val error: AssistantError) : AssistantResult
}

/** Gets an answer from the AI. The ViewModel keeps the conversation; this class only sends it. */
interface AssistantRepository {
    /** [conversation] ends with the user's new question. */
    suspend fun reply(conversation: List<ChatMessage>, language: AppLanguage): AssistantResult
}
