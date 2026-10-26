package com.example.medassist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MedAssistLightColorScheme = lightColorScheme(
    primary = AquaBlue,
    onPrimary = DarkCharcoal,
    primaryContainer = AquaTint,
    onPrimaryContainer = DarkCharcoal,
    secondary = WarmOrange,
    onSecondary = DarkCharcoal,
    secondaryContainer = OrangeTint,
    onSecondaryContainer = DarkCharcoal,
    background = PureWhite,
    onBackground = DarkCharcoal,
    surface = PureWhite,
    onSurface = DarkCharcoal,
    surfaceVariant = CardGrey,
    onSurfaceVariant = MutedGreyStrong,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = CardGrey,
    surfaceContainer = CardGrey,
    surfaceContainerHigh = CardGrey,
    surfaceContainerHighest = CardGrey,
    outline = MutedGrey,
    outlineVariant = CardGrey,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorTint,
    onErrorContainer = DarkCharcoal,
)

private val MedAssistDarkColorScheme = darkColorScheme(
    primary = ColorAquaOnDark,
    onPrimary = DarkCharcoal,
    primaryContainer = ColorAquaContainerDark,
    onPrimaryContainer = LightText,
    secondary = ColorOrangeOnDark,
    onSecondary = DarkCharcoal,
    secondaryContainer = ColorOrangeContainerDark,
    onSecondaryContainer = LightText,
    background = DarkBackground,
    onBackground = LightText,
    surface = DarkSurface,
    onSurface = LightText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF303B3E),
    outline = Color(0xFF879497),
    outlineVariant = Color(0xFF394548),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** App-wide Material 3 theme. The PDF reader opts into [darkTheme] = false locally. */
@Composable
fun MedAssistTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) MedAssistDarkColorScheme else MedAssistLightColorScheme,
        typography = MedAssistTypography,
        shapes = MedAssistShapes,
        content = content,
    )
}


/** Contrast-safe aqua for text/icons (AquaDeep on white, pale aqua on dark surfaces). */
@Composable
fun accessibleAquaTextColor(): Color =
    if (MaterialTheme.colorScheme.background == DarkBackground) ColorAquaOnDark else AquaDeep
