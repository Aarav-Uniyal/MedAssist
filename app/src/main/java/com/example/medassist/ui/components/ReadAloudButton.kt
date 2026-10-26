package com.example.medassist.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist.R
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences

/** Reads the supplied text using the app's selected narration settings. */
@Composable
fun ReadAloudButton(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val preferences = LocalAppPreferences.current
    val settings by preferences.settings.collectAsStateWithLifecycle()
    val narrator = LocalAppNarrator.current
    val locale = LocalConfiguration.current.locales[0]

    if (!settings.narrationEnabled || text.isBlank()) return

    IconButton(
        onClick = {
            narrator.speak(
                text = text,
                languageTag = locale.toLanguageTag(),
                rate = settings.narrationSpeed,
                volume = settings.narrationVolume,
            )
        },
        modifier = if (testTag == null) modifier else modifier.testTag(testTag),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume_up),
            contentDescription = contentDescription,
        )
    }
}
