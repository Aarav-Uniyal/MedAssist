package com.example.medassist.ui.theme

import androidx.compose.ui.graphics.Color

// ── Palette from the project brief ───────────────────────────────────────────
val PureWhite = Color(0xFFFFFFFF)     // primary background
val AquaBlue = Color(0xFF35C6D0)      // primary accent: actions, progress, nav highlight
val WarmOrange = Color(0xFFFF9F43)    // secondary accent: use sparingly
val DarkCharcoal = Color(0xFF263238)  // primary text
val MutedGrey = Color(0xFF687780)     // secondary text, outlines
val CardGrey = Color(0xFFF3F6F7)      // cards and dividers

// ── Accessibility shades derived from the palette ────────────────────────────
// Contrast ratios below are hand-calculated (WCAG 2.x); re-check with a contrast tool.
//
//  Pair                              Ratio   Verdict
//  White text on AquaBlue            ~2.1    FAILS 4.5 -> buttons use DarkCharcoal text instead
//  DarkCharcoal on AquaBlue          ~6.4    OK
//  DarkCharcoal on WarmOrange        ~6.5    OK
//  MutedGrey on white                ~4.6    OK
//  MutedGrey on CardGrey             ~4.3    FAILS 4.5 -> MutedGreyStrong is used on cards
//  AquaBlue as text/icon on white    ~2.1    FAILS -> AquaDeep is used for aqua text and icons
//  WarmOrange as text/icon on white  ~2.1    FAILS -> OrangeDeep is used for orange text and icons
val AquaDeep = Color(0xFF0E7C86)          // ~4.9 on white
val AquaTint = Color(0xFFDDF5F7)          // soft aqua background for icon badges
val OrangeDeep = Color(0xFFB35A00)        // ~4.8 on white
val OrangeTint = Color(0xFFFFF0DD)        // soft orange background for warnings
val MutedGreyStrong = Color(0xFF5F6D75)   // ~4.9 on CardGrey, ~5.3 on white

// ── Not in the brief, but every app needs an error colour ───────────────────
val ErrorRed = Color(0xFFB3261C)          // ~6.5 on white
val ErrorTint = Color(0xFFFCE8E6)


// Dark-mode palette. Accent colours remain vivid enough to distinguish actions on dark surfaces.
val DarkBackground = Color(0xFF101719)
val DarkSurface = Color(0xFF182124)
val DarkSurfaceVariant = Color(0xFF222D30)
val LightText = Color(0xFFF0F5F6)
val LightSecondaryText = Color(0xFFB7C4C7)
val ColorAquaOnDark = Color(0xFF73DDE3)
val ColorAquaContainerDark = Color(0xFF174B50)
val ColorOrangeOnDark = Color(0xFFFFB86E)
val ColorOrangeContainerDark = Color(0xFF573417)
