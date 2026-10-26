package com.example.medassist.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.repository.AuthRepository
import com.example.medassist.data.repository.AuthResult
import com.example.medassist.domain.validation.AuthValidator
import com.example.medassist.domain.validation.FieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: FieldError? = null,
    val error: AuthError? = null,
    val isLoading: Boolean = false,
    /** The address a reset link was requested for (shown in the confirmation). Null until sent. */
    val sentTo: String? = null,
)

class ForgotPasswordViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state: StateFlow<ForgotPasswordUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, error = null) }

    fun submit() {
        val s = _state.value
        if (s.isLoading) return
        val emailError = AuthValidator.validateEmail(s.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        val email = s.email.trim()
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.sendPasswordReset(email)) {
                // Firebase answers "success" even for unknown addresses (so strangers cannot probe
                // for accounts), which is why the confirmation wording says "if an account exists".
                AuthResult.Success -> _state.update { it.copy(isLoading = false, sentTo = email) }
                is AuthResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    /** Back to the form (for "use a different email"), and clears the old state. */
    fun reset() {
        _state.value = ForgotPasswordUiState()
    }
}
