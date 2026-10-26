package com.example.medassist.testutil

import com.example.medassist.data.local.ChatHistoryStore
import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.repository.AssistantRepository
import com.example.medassist.data.repository.AssistantResult
import com.example.medassist.domain.assistant.AppLanguage

class FakeAssistantRepository : AssistantRepository {
    val results = ArrayDeque<AssistantResult>()
    val calls = mutableListOf<Pair<List<ChatMessage>, AppLanguage>>()

    override suspend fun reply(conversation: List<ChatMessage>, language: AppLanguage): AssistantResult {
        calls += conversation to language
        return results.removeFirstOrNull() ?: AssistantResult.Reply("answer ${calls.size}")
    }
}

class FakeChatHistoryStore : ChatHistoryStore {
    val enabled = mutableMapOf<String, Boolean>()
    val saved = mutableMapOf<String, List<ChatMessage>>()

    override fun isEnabled(uid: String) = enabled[uid] ?: false
    override fun setEnabled(uid: String, enabled: Boolean) { this.enabled[uid] = enabled }
    override suspend fun load(uid: String) = saved[uid].orEmpty()
    override suspend fun save(uid: String, messages: List<ChatMessage>) { saved[uid] = messages }
    override suspend fun clear(uid: String) { saved.remove(uid) }
    override fun clearAll() { saved.clear(); enabled.clear() }
}
