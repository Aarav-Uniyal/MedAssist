package com.example.medassist.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.R
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.components.AuthScaffold
import com.example.medassist.ui.components.FormErrorBanner
import com.example.medassist.ui.components.FormInfoBanner
import com.example.medassist.ui.components.MedAssistTextField
import com.example.medassist.ui.components.PrimaryButton
import com.example.medassist.ui.components.SecondaryButton

@Composable
fun ForgotPasswordScreen(
    offline: Boolean,
    onBackToLogin: () -> Unit,
) {
    val viewModel: ForgotPasswordViewModel = viewModel(
        factory = containerViewModelFactory { ForgotPasswordViewModel(it.authRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordContent(
        state = state,
        offline = offline,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::submit,
        onUseDifferentEmail = viewModel::reset,
        onBackToLogin = {
            viewModel.reset()
            onBackToLogin()
        },
    )
}

@Composable
fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    offline: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseDifferentEmail: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val sentTo = state.sentTo
    if (sentTo != null) {
        // Confirmation view. The wording says "if an account exists" on purpose (see the ViewModel).
        AuthScaffold(
            title = stringResource(R.string.forgot_sent_title),
            subtitle = stringResource(R.string.forgot_sent_body, sentTo),
            offline = offline,
        ) {
            FormInfoBanner(stringResource(R.string.forgot_sent_banner))
            PrimaryButton(
                text = stringResource(R.string.action_back_to_sign_in),
                onClick = onBackToLogin,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(R.string.action_use_different_email),
                onClick = onUseDifferentEmail,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }

    AuthScaffold(
        title = stringResource(R.string.forgot_title),
        subtitle = stringResource(R.string.forgot_subtitle),
        offline = offline,
    ) {
        state.error?.let { FormErrorBanner(stringResource(it.messageRes())) }

        MedAssistTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            error = state.emailError?.let { stringResource(it.messageRes()) },
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
        )
        PrimaryButton(
            text = stringResource(R.string.action_send_reset_link),
            onClick = onSubmit,
            loading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.action_back_to_sign_in),
            onClick = onBackToLogin,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
