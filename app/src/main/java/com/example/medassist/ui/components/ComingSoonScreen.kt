package com.example.medassist.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.medassist.R

/**
 * Stand-in body for features that are built in a later phase. It is honest about it: no fake
 * data and no dead buttons. Each feature screen replaces its call to this with the real UI.
 */
@Composable
fun ComingSoonScreen(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { MedAssistTopBar(title = title, onBack = onBack) },
    ) { padding ->
        EmptyState(
            iconRes = iconRes,
            title = stringResource(R.string.section_pending_title),
            message = stringResource(R.string.section_pending_body),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}
