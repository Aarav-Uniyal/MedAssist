package com.example.medassist.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small app-private settings store. No network or Firebase service is used. */
data class AccessibilitySettings(
    val darkMode: Boolean = false,
    val narrationEnabled: Boolean = true,
    val autoNarration: Boolean = false,
    val narrationSpeed: Float = 1.0f,
    val narrationVolume: Float = 1.0f,
)

class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("medassist_preferences", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(readSettings())
    val settings: StateFlow<AccessibilitySettings> = mutableSettings.asStateFlow()

    private fun readSettings() = AccessibilitySettings(
        darkMode = prefs.getBoolean(KEY_DARK_MODE, false),
        narrationEnabled = prefs.getBoolean(KEY_NARRATION_ENABLED, true),
        autoNarration = prefs.getBoolean(KEY_AUTO_NARRATION, false),
        narrationSpeed = prefs.getFloat(KEY_NARRATION_SPEED, 1.0f).coerceIn(0.5f, 2.0f),
        narrationVolume = prefs.getFloat(KEY_NARRATION_VOLUME, 1.0f).coerceIn(0.0f, 1.0f),
    )

    private fun update(transform: (AccessibilitySettings) -> AccessibilitySettings) {
        val updated = synchronized(this) { transform(mutableSettings.value) }
        prefs.edit()
            .putBoolean(KEY_DARK_MODE, updated.darkMode)
            .putBoolean(KEY_NARRATION_ENABLED, updated.narrationEnabled)
            .putBoolean(KEY_AUTO_NARRATION, updated.autoNarration)
            .putFloat(KEY_NARRATION_SPEED, updated.narrationSpeed)
            .putFloat(KEY_NARRATION_VOLUME, updated.narrationVolume)
            .apply()
        mutableSettings.value = updated
    }

    fun setDarkMode(enabled: Boolean) = update { it.copy(darkMode = enabled) }
    fun setNarrationEnabled(enabled: Boolean) = update { it.copy(narrationEnabled = enabled) }
    fun setAutoNarration(enabled: Boolean) = update { it.copy(autoNarration = enabled) }
    fun setNarrationSpeed(speed: Float) = update { it.copy(narrationSpeed = speed.coerceIn(0.5f, 2.0f)) }
    fun setNarrationVolume(volume: Float) = update { it.copy(narrationVolume = volume.coerceIn(0.0f, 1.0f)) }

    private companion object {
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_NARRATION_ENABLED = "narration_enabled"
        const val KEY_AUTO_NARRATION = "auto_narration"
        const val KEY_NARRATION_SPEED = "narration_speed"
        const val KEY_NARRATION_VOLUME = "narration_volume"
    }
}
