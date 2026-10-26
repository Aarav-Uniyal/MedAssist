package com.example.medassist.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.example.medassist.ui.components.MedAssistTextField
import com.example.medassist.ui.components.PrimaryButton
import com.example.medassist.ui.theme.accessibleAquaTextColor
import com.example.medassist.ui.theme.Spacing

/** Stateful wrapper: finds the ViewModel and hands plain values and callbacks to [LoginContent]. */
@Composable
fun LoginScreen(
    offline: Boolean,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
    val viewModel: LoginViewModel = viewModel(
        factory = containerViewModelFactory { LoginViewModel(it.authRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginContent(
        state = state,
        offline = offline,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisible = viewModel::togglePasswordVisible,
        onSubmit = viewModel::submit,
        onForgotPassword = onForgotPassword,
        onRegister = onRegister,
    )
}

/** Stateless UI. Takes everything as parameters, so tests and previews can drive it directly. */
@Composable
fun LoginContent(
    state: LoginUiState,
    offline: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.login_title),
        subtitle = stringResource(R.string.login_subtitle),
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
        )
        MedAssistTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            error = state.passwordError?.let { stringResource(it.messageRes()) },
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            isPassword = true,
            passwordVisible = state.passwordVisible,
            onTogglePasswordVisible = onTogglePasswordVisible,
        )

        TextButton(onClick = onForgotPassword, enabled = !state.isLoading) {
            Text(text = stringResource(R.string.action_forgot_password), color = accessibleAquaTextColor())
        }

        PrimaryButton(
            text = stringResource(R.string.action_sign_in),
            onClick = onSubmit,
            loading = state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.login_no_account),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRegister, enabled = !state.isLoading) {
                Text(text = stringResource(R.string.action_register), color = accessibleAquaTextColor())
            }
        }
    }
}
