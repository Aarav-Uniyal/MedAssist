package com.example.medassist.ui.chatbot

import com.example.medassist.R
import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.model.ChatRole
import com.example.medassist.data.repository.AssistantError
import com.example.medassist.data.repository.AssistantResult
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.domain.assistant.AssistantConfig
import com.example.medassist.domain.assistant.RateLimiter
import com.example.medassist.testutil.FakeAssistantRepository
import com.example.medassist.testutil.FakeChatHistoryStore
import com.example.medassist.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val repo = FakeAssistantRepository()
    private val store = FakeChatHistoryStore()
    private val online = MutableStateFlow(true)
    private var language = AppLanguage.ENGLISH
    private var ids = 0

    private fun vm(limiter: RateLimiter = RateLimiter(maxRequests = 100)) = ChatViewModel(
        uid = "u1", repository = repo, historyStore = store, isOnline = online,
        language = { language }, clock = { 1000L }, rateLimiter = limiter, newId = { "m${ids++}" },
    )

    private fun ChatViewModel.say(text: String) {
        onInputChange(text)
        send()
    }

    @Test fun send_addsQuestionAndReply_andClearsInput() = runTest {
        val v = vm()
        v.say("  What is HbA1c?  ")
        val s = v.state.value
        assertEquals(listOf(ChatRole.USER, ChatRole.ASSISTANT), s.messages.map { it.role })
        assertEquals("What is HbA1c?", s.messages[0].text)
        assertEquals("", s.input)
        assertFalse(s.sending)
    }

    @Test fun conversationContext_isSent_inChosenLanguage() = runTest {
        val v = vm()
        v.say("first")
        language = AppLanguage.HINDI
        v.say("second")
        val (conversation, lang) = repo.calls.last()
        assertEquals(listOf("first", "answer 1", "second"), conversation.map { it.text })
        assertEquals(AppLanguage.HINDI, lang)
    }

    @Test fun blankInput_isIgnored() = runTest {
        val v = vm()
        v.say("   ")
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun tooLong_isRejected() = runTest {
        val v = vm()
        v.say("a".repeat(AssistantConfig.MAX_MESSAGE_CHARS + 1))
        assertEquals(R.string.chat_error_too_long, v.state.value.inputErrorRes)
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun offline_keepsInput_andExplains() = runTest {
        online.value = false
        val v = vm()
        v.say("hello")
        assertEquals(R.string.chat_error_offline, v.state.value.errorRes)
        assertEquals("hello", v.state.value.input)
        assertTrue(repo.calls.isEmpty())
    }

    @Test fun rateLimit_blocksExtraQuestions() = runTest {
        val v = vm(RateLimiter(maxRequests = 1, clock = { 0L }))
        v.say("one")
        v.say("two")
        assertEquals(R.string.chat_error_rate_limited, v.state.value.errorRes)
        assertEquals(1, repo.calls.size)
    }

    @Test fun failure_marksQuestion_thenRetryWorks() = runTest {
        repo.results.add(AssistantResult.Failure(AssistantError.NETWORK))
        val v = vm()
        v.say("question")
        val failed = v.state.value.messages.single()
        assertTrue(failed.failed)
        assertEquals(R.string.chat_error_network, v.state.value.errorRes)
        v.retry(failed.id)
        val s = v.state.value
        assertEquals(listOf("question", "answer 2"), s.messages.map { it.text })
        assertTrue(s.messages.none { it.failed })
        assertNull(s.errorRes)
    }

    @Test fun emergencyWords_showBanner() = runTest {
        val v = vm()
        v.say("my father has chest pain")
        assertTrue(v.state.value.showEmergency)
        v.dismissEmergency()
        assertFalse(v.state.value.showEmergency)
    }

    @Test fun history_isNotSavedByDefault() = runTest {
        val v = vm()
        v.say("private question")
        assertTrue(store.saved.isEmpty())
    }

    @Test fun history_savedWhenEnabled_andRestored_andClearedOnNewConversation() = runTest {
        val v = vm()
        v.setHistoryEnabled(true)
        v.say("keep this")
        assertEquals(2, store.saved["u1"]?.size)

        val restored = vm()
        assertEquals(listOf("keep this", "answer 1"), restored.state.value.messages.map { it.text })

        restored.requestNewConversation()
        assertTrue(restored.state.value.confirmNewConversation)
        restored.confirmNewConversation()
        assertTrue(restored.state.value.messages.isEmpty())
        assertNull(store.saved["u1"])
    }

    @Test fun turningHistoryOff_deletesSavedChat() = runTest {
        store.enabled["u1"] = true
        store.saved["u1"] = listOf(ChatMessage("x", ChatRole.USER, "old", 1L))
        val v = vm()
        v.setHistoryEnabled(false)
        assertNull(store.saved["u1"])
    }
}
