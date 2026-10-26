package com.example.medassist.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.R
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.components.AuthScaffold
import com.example.medassist.ui.components.FormErrorBanner
import com.example.medassist.ui.components.FormInfoBanner
import com.example.medassist.ui.components.PrimaryButton
import com.example.medassist.ui.components.SecondaryButton

/**
 * Shown while someone is signed in but has not confirmed their email. Nothing else in the app
 * is reachable until they do (see MedAssistApp).
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    offline: Boolean,
) {
    // Keyed by email so a different person signing in later never sees the previous screen's state.
    val viewModel: VerifyEmailViewModel = viewModel(
        key = "verify:$email",
        factory = containerViewModelFactory { VerifyEmailViewModel(it.authRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // When the user comes back from their email app, check automatically (and quietly).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.check(manual = false) }

    VerifyEmailContent(
        email = email,
        state = state,
        offline = offline,
        onCheck = { viewModel.check(manual = true) },
        onResend = viewModel::resend,
        onUseOtherAccount = viewModel::signOut,
    )
}

@Composable
fun VerifyEmailContent(
    email: String,
    state: VerifyEmailUiState,
    offline: Boolean,
    onCheck: () -> Unit,
    onResend: () -> Unit,
    onUseOtherAccount: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.verify_title),
        subtitle = stringResource(R.string.verify_body, email),
        offline = offline,
    ) {
        state.errorRes?.let { FormErrorBanner(stringResource(it)) }
        state.infoRes?.let { FormInfoBanner(stringResource(it)) }

        PrimaryButton(
            text = stringResource(R.string.action_verify_done),
            onClick = onCheck,
            loading = state.isChecking,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = if (state.cooldownSeconds > 0) {
                stringResource(R.string.verify_resend_wait, state.cooldownSeconds)
            } else {
                stringResource(R.string.action_resend)
            },
            onClick = onResend,
            enabled = state.cooldownSeconds == 0,
            loading = state.isResending,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.action_use_other_account),
            onClick = onUseOtherAccount,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
