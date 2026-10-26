package com.example.medassist.ui.theme

import androidx.compose.ui.unit.dp

/** One spacing scale for the whole app, so layouts stay consistent. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp

    /** Android accessibility guideline: tappable things are at least 48dp x 48dp. */
    val minTouchTarget = 48.dp
}
