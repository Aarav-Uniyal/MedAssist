package com.example.medassist.data.local

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * App-scoped Android TextToSpeech wrapper. Speech is split into sentences so the Pause/Resume
 * controls can safely resume at the current sentence (the Android TTS API itself has no pause API).
 */
data class NarrationState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val failed: Boolean = false,
)

class AppNarrator(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mutableState = MutableStateFlow(NarrationState())
    val state: StateFlow<NarrationState> = mutableState.asStateFlow()

    private var engine: TextToSpeech? = null
    private var initialized = false
    private var pendingRequest: Request? = null
    private var chunks: List<String> = emptyList()
    private var chunkIndex = 0
    private var currentUtteranceId = ""
    private var generation = 0
    private var utteranceSequence = 0
    private var paused = false
    private var rate = 1.0f
    private var volume = 1.0f
    private var languageTag = "en"

    private data class Request(
        val text: String,
        val languageTag: String,
        val rate: Float,
        val volume: Float,
    )

    @Synchronized
    fun speak(text: String, languageTag: String, rate: Float, volume: Float) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val request = Request(
            text = cleanText,
            languageTag = languageTag.ifBlank { Locale.getDefault().toLanguageTag() },
            rate = rate.coerceIn(0.5f, 2.0f),
            volume = volume.coerceIn(0.0f, 1.0f),
        )
        ensureEngine()
        if (!initialized) {
            pendingRequest = request
        } else {
            begin(request)
        }
    }

    @Synchronized
    private fun ensureEngine() {
        if (engine == null) {
            engine = TextToSpeech(appContext, this).also { tts ->
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post {
                            if (utteranceId == currentUtteranceId && !paused) {
                                mutableState.value = mutableState.value.copy(isSpeaking = true, isPaused = false)
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            if (utteranceId == currentUtteranceId && !paused) {
                                chunkIndex++
                                speakCurrentChunk()
                            }
                        }
                    }

                    @Deprecated("Deprecated by Android; required for API compatibility")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            if (utteranceId == currentUtteranceId && !paused) {
                                mutableState.value = mutableState.value.copy(isSpeaking = false, failed = true)
                            }
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post {
                            if (utteranceId == currentUtteranceId && !paused) {
                                mutableState.value = mutableState.value.copy(isSpeaking = false, failed = true)
                            }
                        }
                    }
                })
            }
        }
    }

    override fun onInit(status: Int) {
        mainHandler.post {
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                mutableState.value = NarrationState(isReady = true)
                val request = pendingRequest
                pendingRequest = null
                if (request != null) begin(request)
            } else {
                initialized = false
                mutableState.value = NarrationState(failed = true)
            }
        }
    }

    @Synchronized
    private fun begin(request: Request) {
        currentUtteranceId = "replacing_$generation" // Ignore callbacks from the previous request.
        engine?.stop()
        generation++
        currentUtteranceId = "starting_$generation"
        languageTag = request.languageTag
        rate = request.rate
        volume = request.volume
        chunks = request.text
            .split(Regex("(?<=[.!?।])\\s+"))
            .flatMap { sentence -> if (sentence.length <= 350) listOf(sentence) else sentence.chunked(300) }
            .filter { it.isNotBlank() }
        chunkIndex = 0
        paused = false
        mutableState.value = mutableState.value.copy(isReady = true, isSpeaking = false, isPaused = false, failed = false)
        speakCurrentChunk()
    }

    @Synchronized
    private fun speakCurrentChunk() {
        val tts = engine ?: return
        if (!initialized || paused) return
        if (chunkIndex >= chunks.size) {
            mutableState.value = mutableState.value.copy(isSpeaking = false, isPaused = false)
            return
        }

        val locale = Locale.forLanguageTag(languageTag)
        val languageResult = tts.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Keep narration usable on devices without the selected voice data.
            val fallback = if (locale.language == "hi") Locale("hi", "IN") else Locale.getDefault()
            val fallbackResult = tts.setLanguage(fallback)
            if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                mutableState.value = mutableState.value.copy(isSpeaking = false, failed = true)
                return
            }
        }
        tts.setSpeechRate(rate)
        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
        utteranceSequence++
        currentUtteranceId = "medassist_${generation}_${chunkIndex}_$utteranceSequence"
        val result = tts.speak(chunks[chunkIndex], TextToSpeech.QUEUE_FLUSH, params, currentUtteranceId)
        if (result == TextToSpeech.ERROR) {
            mutableState.value = mutableState.value.copy(isSpeaking = false, failed = true)
        } else {
            mutableState.value = mutableState.value.copy(isSpeaking = true, isPaused = false, failed = false)
        }
    }

    @Synchronized
    fun pause() {
        if (!mutableState.value.isSpeaking || paused) return
        paused = true
        engine?.stop()
        mutableState.value = mutableState.value.copy(isSpeaking = false, isPaused = true)
    }

    @Synchronized
    fun resume() {
        if (!paused) return
        paused = false
        speakCurrentChunk() // resumes from the current sentence; it may repeat that sentence
    }

    @Synchronized
    fun stop() {
        generation++
        currentUtteranceId = "stopped_$generation" // Ignore callbacks from the utterance being cancelled.
        engine?.stop()
        chunks = emptyList()
        chunkIndex = 0
        pendingRequest = null
        paused = false
        mutableState.value = mutableState.value.copy(isSpeaking = false, isPaused = false, failed = false)
    }

    @Synchronized
    fun release() {
        stop()
        engine?.shutdown()
        engine = null
        initialized = false
        mutableState.value = NarrationState()
    }
}
