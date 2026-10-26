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

@Composable
fun RegisterScreen(
    offline: Boolean,
    onBackToLogin: () -> Unit,
) {
    val viewModel: RegisterViewModel = viewModel(
        factory = containerViewModelFactory { RegisterViewModel(it.authRepository) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterContent(
        state = state,
        offline = offline,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onTogglePasswordVisible = viewModel::togglePasswordVisible,
        onSubmit = viewModel::submit,
        onBackToLogin = onBackToLogin,
    )
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    offline: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    AuthScaffold(
        title = stringResource(R.string.register_title),
        subtitle = stringResource(R.string.register_subtitle),
        offline = offline,
    ) {
        state.error?.let { FormErrorBanner(stringResource(it.messageRes())) }

        MedAssistTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_name),
            error = state.nameError?.let { stringResource(it.messageRes()) },
            enabled = !state.isLoading,
        )
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
            // The rule is shown up front, and replaced by the specific error if the input breaks it.
            supportingText = stringResource(R.string.register_password_hint),
            error = state.passwordError?.let { stringResource(it.messageRes()) },
            enabled = !state.isLoading,
            isPassword = true,
            passwordVisible = state.passwordVisible,
            onTogglePasswordVisible = onTogglePasswordVisible,
        )
        MedAssistTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmChange,
            label = stringResource(R.string.field_confirm_password),
            error = state.confirmError?.let { stringResource(it.messageRes()) },
            enabled = !state.isLoading,
            imeAction = ImeAction.Done,
            onImeAction = onSubmit,
            isPassword = true,
            passwordVisible = state.passwordVisible,
            onTogglePasswordVisible = onTogglePasswordVisible,
        )

        PrimaryButton(
            text = stringResource(R.string.action_register),
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
                text = stringResource(R.string.register_have_account),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onBackToLogin, enabled = !state.isLoading) {
                Text(text = stringResource(R.string.action_sign_in), color = accessibleAquaTextColor())
            }
        }
    }
}
