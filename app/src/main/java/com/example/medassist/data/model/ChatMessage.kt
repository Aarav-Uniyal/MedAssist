package com.example.medassist.data.model

enum class ChatRole { USER, ASSISTANT }

/** One chat bubble. [failed] marks a question that never got an answer and can be retried. */
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val timeMillis: Long,
    val failed: Boolean = false,
)
