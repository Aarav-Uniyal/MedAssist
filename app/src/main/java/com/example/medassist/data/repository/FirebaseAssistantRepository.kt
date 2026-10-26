package com.example.medassist.data.repository

import android.util.Log
import com.example.medassist.data.model.ChatMessage
import com.example.medassist.data.model.ChatRole
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.domain.assistant.AssistantConfig
import com.example.medassist.domain.assistant.AssistantPrompt
import com.example.medassist.domain.assistant.ReplyCleaner
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Talks to Gemini through Firebase AI Logic using the free "Gemini Developer API" tier.
 *
 * Why this counts as a secure backend on the free Spark plan: the Gemini API key never exists in the app.
 * Firebase AI Logic is a Google-run proxy that holds the key, checks App Check (so only the real app can call
 * it) and applies per-user rate limits that you set in the console. No Cloud Function is needed.
 */
class FirebaseAssistantRepository(
    private val modelName: suspend () -> String,
) : AssistantRepository {

    private val lock = Mutex()
    private var cachedKey: String? = null
    private var cachedModel: GenerativeModel? = null

    override suspend fun reply(conversation: List<ChatMessage>, language: AppLanguage): AssistantResult {
        return try {
            val name = modelName()
            Log.d(TAG, "Sending ${contextWindow(conversation).size} messages to $name")
            val model = modelFor(name, language)
            val contents = contextWindow(conversation).map { message ->
                content(role = if (message.role == ChatRole.USER) "user" else "model") { text(message.text) }
            }
            val text = model.generateContent(contents).text?.let(ReplyCleaner::clean)
            if (text.isNullOrBlank()) AssistantResult.Failure(AssistantError.EMPTY_REPLY) else AssistantResult.Reply(text)
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Assistant request timed out")
            AssistantResult.Failure(AssistantError.TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never log the conversation itself: it may contain health information.
            // The error text only describes the failure (status code, App Check, quota), not the chat.
            Log.w(TAG, "Assistant request failed: ${e.javaClass.simpleName}: ${e.message?.take(300)}")
            AssistantResult.Failure(assistantErrorFor(e))
        }
    }

    private suspend fun modelFor(name: String, language: AppLanguage): GenerativeModel = lock.withLock {
        val key = "$name|${language.tag}"
        cachedModel?.takeIf { cachedKey == key } ?: Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = name,
                generationConfig = generationConfig {
                    temperature = 0.4f
                    maxOutputTokens = AssistantConfig.MAX_REPLY_TOKENS
                    // Flash-Lite supports MINIMAL thinking; Flash supports LOW. Match the selected model
                    // so switching models through Remote Config does not cause a 400 response.
                    thinkingConfig = thinkingConfig {
                        thinkingLevel = if (name.endsWith("-flash-lite")) {
                            ThinkingLevel.MINIMAL
                        } else {
                            ThinkingLevel.LOW
                        }
                    }
                },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, HarmBlockThreshold.MEDIUM_AND_ABOVE),
                ),
                systemInstruction = content { text(AssistantPrompt.systemInstruction(language)) },
                requestOptions = RequestOptions(timeoutInMillis = AssistantConfig.REQUEST_TIMEOUT_MILLIS),
            ).also {
                cachedKey = key
                cachedModel = it
            }
    }

    companion object {
        private const val TAG = "MedAssistAI"

        /**
         * The part of the chat that is sent: answered turns plus the new question, at most
         * [AssistantConfig.MAX_CONTEXT_MESSAGES] messages, starting with a user message.
         */
        fun contextWindow(conversation: List<ChatMessage>): List<ChatMessage> =
            conversation.filterNot { it.failed }
                .takeLast(AssistantConfig.MAX_CONTEXT_MESSAGES)
                .dropWhile { it.role != ChatRole.USER }
    }
}
