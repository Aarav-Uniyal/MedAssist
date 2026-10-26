package com.example.medassist.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import java.util.Locale

/** Speaks important, newly displayed status/error text when narration is enabled. */
@Composable
fun AnnounceForNarration(text: String, enabled: Boolean = true) {
    val preferences = LocalAppPreferences.current
    val settings by preferences.settings.collectAsStateWithLifecycle()
    val narrator = LocalAppNarrator.current
    val configuration = LocalConfiguration.current
    val languageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        .ifBlank { configuration.locales[0]?.toLanguageTag() ?: Locale.getDefault().toLanguageTag() }

    LaunchedEffect(text, enabled, settings.narrationEnabled) {
        if (enabled && settings.narrationEnabled && text.isNotBlank()) {
            narrator.speak(text, languageTag, settings.narrationSpeed, settings.narrationVolume)
        }
    }
}
