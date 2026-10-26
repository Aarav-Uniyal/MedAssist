package com.example.medassist.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.medassist.data.local.AppNarrator
import com.example.medassist.data.local.AppPreferences

val LocalAppPreferences = staticCompositionLocalOf<AppPreferences> {
    error("AppPreferences was not provided")
}

val LocalAppNarrator = staticCompositionLocalOf<AppNarrator> {
    error("AppNarrator was not provided")
}
