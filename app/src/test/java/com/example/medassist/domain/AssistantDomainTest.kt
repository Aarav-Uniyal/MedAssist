package com.example.medassist.domain

import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.model.ChatRole
import com.example.medassist.data.repository.AssistantError
import com.example.medassist.data.repository.FirebaseAssistantRepository
import com.example.medassist.data.repository.assistantErrorFor
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.domain.assistant.AssistantConfig
import com.example.medassist.domain.assistant.AssistantPrompt
import com.example.medassist.domain.assistant.EmergencyDetector
import com.example.medassist.domain.assistant.RateLimiter
import com.example.medassist.domain.assistant.ReplyCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

class AssistantDomainTest {
    @Test fun prompt_containsSafetyRules_andLanguage() {
        val en = AssistantPrompt.systemInstruction(AppLanguage.ENGLISH)
        listOf("not a doctor", "definitive diagnosis", "Never invent", "Do not prescribe", "112", "personal details")
            .forEach { assertTrue(it, en.contains(it)) }
        assertTrue(en.contains("Reply in English"))
        assertTrue(AssistantPrompt.systemInstruction(AppLanguage.HINDI).contains("Reply in Hindi (Devanagari script)"))
    }

    @Test fun remoteConfigModelName_usesAllowedModelsAndSafeDefault() {
        assertEquals("gemini-3.5-flash-lite", AssistantConfig.resolveModelName(""))
        assertEquals("gemini-3.5-flash-lite", AssistantConfig.resolveModelName("unknown-model"))
        assertEquals("gemini-3.5-flash-lite", AssistantConfig.resolveModelName(" gemini-3.5-flash-lite "))
        assertEquals("gemini-3.8-flash", AssistantConfig.resolveModelName("gemini-3.8-flash"))
        assertEquals(
            "gemini-3.5-flash-lite",
            AssistantConfig.resolveConfiguredModelName("gemini-3.5-flash-lite", "gemini-3.8-flash"),
        )
        assertEquals(
            "gemini-3.8-flash",
            AssistantConfig.resolveConfiguredModelName("", "gemini-3.8-flash"),
        )
    }

    @Test fun language_fromTag() {
        assertEquals(AppLanguage.HINDI, AppLanguage.fromTag("hi"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromTag("hi-IN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("fr"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(null))
    }

    @Test fun emergency_detectsEnglishAndHindi() {
        assertTrue(EmergencyDetector.looksUrgent("I have CHEST PAIN and sweating"))
        assertTrue(EmergencyDetector.looksUrgent("my dad can’t breathe"))
        assertTrue(EmergencyDetector.looksUrgent("मुझे सीने में दर्द हो रहा है"))
        assertTrue(EmergencyDetector.looksUrgent("I want to kill myself"))
        assertFalse(EmergencyDetector.looksUrgent("What does HbA1c mean?"))
        assertFalse(EmergencyDetector.looksUrgent("बेहतर नींद के लिए सुझाव"))
    }

    @Test fun rateLimiter_allowsBurstThenWaitsForWindow() {
        var now = 0L
        val limiter = RateLimiter(maxRequests = 2, windowMillis = 1000, clock = { now })
        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())
        now = 999
        assertFalse(limiter.tryAcquire())
        now = 1000
        assertTrue(limiter.tryAcquire())
        limiter.release()
        assertTrue(limiter.tryAcquire())
    }

    @Test fun replyCleaner_removesMarkdown() {
        assertEquals(
            "Iron\n\n- Eat **greens**\n- Rest\n\nSee a doctor.",
            ReplyCleaner.clean("## Iron\n\n\n\n* Eat ****greens****\n+ Rest\n\nSee a **doctor**."),
        )
        assertEquals("Plain text", ReplyCleaner.clean("  Plain text  "))
    }

    private fun msg(i: Int, role: ChatRole, failed: Boolean = false) = ChatMessage("$i", role, "m$i", i.toLong(), failed)

    @Test fun contextWindow_skipsFailed_limitsSize_startsWithUser() {
        val chat = (0 until 30).map { msg(it, if (it % 2 == 0) ChatRole.USER else ChatRole.ASSISTANT) } +
            msg(30, ChatRole.USER, failed = true) + msg(31, ChatRole.USER)
        val window = FirebaseAssistantRepository.contextWindow(chat)
        assertTrue(window.size <= AssistantConfig.MAX_CONTEXT_MESSAGES)
        assertEquals(ChatRole.USER, window.first().role)
        assertEquals("31", window.last().id)
        assertTrue(window.none { it.failed })
    }

    @Test fun errorMapping() {
        assertEquals(AssistantError.NETWORK, assistantErrorFor(IOException("no route")))
        assertEquals(AssistantError.TIMEOUT, assistantErrorFor(SocketTimeoutException()))
        assertEquals(AssistantError.NETWORK, assistantErrorFor(RuntimeException("wrap", IOException())))
        assertEquals(AssistantError.APP_CHECK, assistantErrorFor(RuntimeException("Firebase App Check token is invalid")))
        assertEquals(AssistantError.QUOTA, assistantErrorFor(RuntimeException("429 RESOURCE_EXHAUSTED")))
        assertEquals(AssistantError.UNKNOWN, assistantErrorFor(IllegalStateException("?")))
    }
}
