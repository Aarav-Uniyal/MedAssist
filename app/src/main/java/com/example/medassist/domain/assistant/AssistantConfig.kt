package com.example.medassist.domain.assistant

/** Limits and defaults for the AI Assistant. The model itself can be changed in Remote Config. */
object AssistantConfig {
    /** Cost-sensitive default model on the Gemini Developer API free tier. */
    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
    /** Preferred key described in the current setup instructions. */
    const val PREFERRED_REMOTE_MODEL_KEY = "gemini_model_name"
    /** Backward-compatible key already used by earlier project versions. */
    const val REMOTE_MODEL_KEY = "assistant_model"

    /**
     * Only allow model IDs this app has deliberately configured and tested. This prevents a typo
     * or stale Remote Config value from breaking every assistant request.
     */
    val SUPPORTED_MODELS: Set<String> = setOf(
        DEFAULT_MODEL,
        "gemini-3.8-flash",
    )

    fun resolveConfiguredModelName(preferredValue: String, legacyValue: String): String =
        resolveModelName(preferredValue.trim().ifBlank { legacyValue })

    fun resolveModelName(configuredValue: String): String =
        configuredValue.trim().takeIf { it in SUPPORTED_MODELS } ?: DEFAULT_MODEL

    const val MAX_MESSAGE_CHARS = 1000

    /** Only the most recent messages are sent as context, never the whole history. */
    const val MAX_CONTEXT_MESSAGES = 20

    const val MAX_REPLY_TOKENS = 2048
    const val REQUEST_TIMEOUT_MILLIS = 60_000L

    /** At most this many questions per minute from one phone (a second limit is set in the console). */
    const val RATE_LIMIT_COUNT = 5
    const val RATE_LIMIT_WINDOW_MILLIS = 60_000L
}

/** The language replies are written in. */
enum class AppLanguage(val tag: String, val englishName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            if (tag != null && tag.lowercase().startsWith("hi")) HINDI else ENGLISH
    }
}
