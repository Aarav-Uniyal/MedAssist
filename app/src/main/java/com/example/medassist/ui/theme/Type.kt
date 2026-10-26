package com.example.medassist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private val Base = Typography()

/**
 * Letter spacing is forced to 0 everywhere. Non-zero tracking breaks the joined letter shapes
 * (conjuncts) in Devanagari, so Hindi text would look wrong with Material's default spacing.
 * Line heights are generous (about 1.5x) because Devanagari has tall marks above and below the line.
 * Sizes are in sp, so they follow the user's system font-size setting.
 */
private fun TextStyle.style(size: TextUnit, line: TextUnit, weight: FontWeight): TextStyle =
    copy(fontSize = size, lineHeight = line, fontWeight = weight, letterSpacing = 0.sp)

private fun TextStyle.noTracking(): TextStyle = copy(letterSpacing = 0.sp)

val MedAssistTypography = Typography(
    displayLarge = Base.displayLarge.noTracking(),
    displayMedium = Base.displayMedium.noTracking(),
    displaySmall = Base.displaySmall.noTracking(),
    headlineLarge = Base.headlineLarge.noTracking(),
    headlineMedium = Base.headlineMedium.style(28.sp, 40.sp, FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.style(24.sp, 34.sp, FontWeight.SemiBold),
    titleLarge = Base.titleLarge.style(22.sp, 32.sp, FontWeight.SemiBold),
    titleMedium = Base.titleMedium.style(18.sp, 26.sp, FontWeight.SemiBold),
    titleSmall = Base.titleSmall.style(16.sp, 24.sp, FontWeight.Medium),
    bodyLarge = Base.bodyLarge.style(16.sp, 26.sp, FontWeight.Normal),
    bodyMedium = Base.bodyMedium.style(14.sp, 22.sp, FontWeight.Normal),
    bodySmall = Base.bodySmall.style(12.sp, 18.sp, FontWeight.Normal),
    labelLarge = Base.labelLarge.style(14.sp, 20.sp, FontWeight.SemiBold),
    labelMedium = Base.labelMedium.style(12.sp, 18.sp, FontWeight.Medium),
    labelSmall = Base.labelSmall.style(11.sp, 16.sp, FontWeight.Medium),
)
