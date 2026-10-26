package com.example.medassist.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.medassist.ui.theme.AquaDeep
import com.example.medassist.ui.theme.AquaTint
import com.example.medassist.ui.theme.OrangeDeep
import com.example.medassist.ui.theme.OrangeTint
import com.example.medassist.ui.theme.Spacing

/**
 * Rounded light-grey card. Pass [onClick] to make the whole card one tappable control;
 * TalkBack then reads its text as a single item instead of many small ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedAssistCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow, contentColor = MaterialTheme.colorScheme.onSurface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(Spacing.md), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            elevation = elevation,
        ) {
            Column(modifier = Modifier.padding(Spacing.md), content = content)
        }
    }
}

/** Which accent colour a card uses. Orange is for highlights only. */
enum class CardAccent { Aqua, Orange }

/** Home-screen shortcut: icon, title and one short line. */
@Composable
fun QuickAccessCard(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: CardAccent = CardAccent.Aqua,
) {
    val badgeBackground: Color = if (accent == CardAccent.Aqua) AquaTint else OrangeTint
    val badgeTint: Color = if (accent == CardAccent.Aqua) AquaDeep else OrangeDeep

    MedAssistCard(modifier = modifier, onClick = onClick) {
        IconBadge(iconRes = iconRes, background = badgeBackground, tint = badgeTint)
        Spacer(Modifier.height(Spacing.sm))
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
