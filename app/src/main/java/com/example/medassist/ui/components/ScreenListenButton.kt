package com.example.medassist.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist.R
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/** Visible, TalkBack-labelled screen narration action. */
@Composable
fun ScreenListenButton(narrationText: String) {
    val preferences = LocalAppPreferences.current
    val settings by preferences.settings.collectAsStateWithLifecycle()
    val narrator = LocalAppNarrator.current
    if (!settings.narrationEnabled) return

    IconButton(
        onClick = {
            val languageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                .ifBlank { Locale.getDefault().toLanguageTag() }
            narrator.speak(
                text = narrationText,
                languageTag = languageTag,
                rate = settings.narrationSpeed,
                volume = settings.narrationVolume,
            )
        },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = stringResource(R.string.a11y_listen_to_screen),
        )
    }
}
