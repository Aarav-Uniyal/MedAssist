package com.example.medassist.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medassist.R
import com.example.medassist.ui.common.UiState
import com.example.medassist.ui.theme.AquaBlue
import com.example.medassist.ui.theme.AquaDeep
import com.example.medassist.ui.theme.AquaTint
import com.example.medassist.ui.theme.CardGrey
import com.example.medassist.ui.theme.DarkCharcoal
import com.example.medassist.ui.theme.OrangeDeep
import com.example.medassist.ui.theme.OrangeTint
import com.example.medassist.ui.theme.Spacing

/**
 * Spinner plus a text message. The text matters: colour alone (an aqua spinner) is not enough
 * for people with low vision, and TalkBack announces the message when it appears.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.common_loading),
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.lg)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = AquaBlue, trackColor = CardGrey, strokeWidth = 4.dp)
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Nothing here yet" screen. Tell the user what to do next via [actionLabel] and [onAction]. */
@Composable
fun EmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    CenteredMessage(
        iconRes = iconRes,
        badgeBackground = AquaTint,
        badgeTint = AquaDeep,
        title = title,
        message = message,
        modifier = modifier,
    ) {
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

/** Something failed. Always explain what happened and offer [onRetry] when retrying makes sense. */
@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.common_error_title),
    onRetry: (() -> Unit)? = null,
) {
    AnnounceForNarration("$title. $message")
    CenteredMessage(
        iconRes = R.drawable.ic_warning,
        badgeBackground = OrangeTint,
        badgeTint = OrangeDeep,
        title = title,
        message = message,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (onRetry != null) {
            Spacer(Modifier.height(Spacing.lg))
            PrimaryButton(text = stringResource(R.string.common_retry), onClick = onRetry)
        }
    }
}

/** Thin bar shown at the top of the app while the phone has no working internet connection. */
@Composable
fun OfflineBanner(visible: Boolean, modifier: Modifier = Modifier) {
    if (visible) AnnounceForNarration(stringResource(R.string.common_offline_banner))
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrangeTint)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The icon means the banner is not conveyed by colour alone.
            Icon(
                painter = painterResource(R.drawable.ic_warning),
                contentDescription = null,
                tint = OrangeDeep,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.common_offline_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = DarkCharcoal,
            )
        }
    }
}

/**
 * Draws the right thing for each [UiState]. Later screens use it like this:
 *
 *     UiStateContent(state, onRetry = viewModel::reload, empty = { EmptyState(...) }) { items -> ... }
 */
@Composable
fun <T> UiStateContent(
    state: UiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    empty: @Composable () -> Unit,
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier) {
        when (state) {
            UiState.Loading -> LoadingState(Modifier.fillMaxSize())
            UiState.Empty -> empty()
            is UiState.Error -> ErrorState(
                message = stringResource(state.messageRes),
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            is UiState.Success -> content(state.data)
        }
    }
}

/** Shared layout for the empty and error screens. Scrolls, so large font sizes never clip text. */
@Composable
private fun CenteredMessage(
    @DrawableRes iconRes: Int,
    badgeBackground: Color,
    badgeTint: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconBadge(
            iconRes = iconRes,
            background = badgeBackground,
            tint = badgeTint,
            badgeSize = 72.dp,
            iconSize = 32.dp,
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}
