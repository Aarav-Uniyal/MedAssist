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

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val nameError: FieldError? = null,
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val confirmError: FieldError? = null,
    val error: AuthError? = null,
    val isLoading: Boolean = false,
)

class RegisterViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onNameChange(value: String) =
        _state.update { it.copy(name = value, nameError = null, error = null) }

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, confirmError = null, error = null) }

    fun onConfirmChange(value: String) =
        _state.update { it.copy(confirmPassword = value, confirmError = null, error = null) }

    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit() {
        val s = _state.value
        if (s.isLoading) return

        val nameError = AuthValidator.validateName(s.name)
        val emailError = AuthValidator.validateEmail(s.email)
        val passwordError = AuthValidator.validateNewPassword(s.password)
        val confirmError = AuthValidator.validateConfirm(s.password, s.confirmPassword)
        if (listOf(nameError, emailError, passwordError, confirmError).any { it != null }) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                )
            }
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = repository.register(s.name.trim(), s.email.trim(), s.password)) {
                // The session flow moves on to the verification screen.
                AuthResult.Success -> _state.value = RegisterUiState()
                is AuthResult.Failure -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
