package com.example.medassist.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.R
import com.example.medassist.data.repository.AuthError
import com.example.medassist.data.repository.AuthRepository
import com.example.medassist.data.repository.AuthResult
import com.example.medassist.ui.auth.messageRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val showDeleteDialog: Boolean = false,
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isDeleting: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

class ProfileViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun signOut() = repository.signOut()

    fun openDeleteDialog() = _state.update { ProfileUiState(showDeleteDialog = true) }

    fun dismissDeleteDialog() {
        if (_state.value.isDeleting) return // cannot cancel halfway through
        _state.value = ProfileUiState()
    }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errorRes = null) }

    fun togglePasswordVisible() = _state.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun confirmDelete() {
        val s = _state.value
        if (s.isDeleting) return
        if (s.password.isEmpty()) {
            _state.update { it.copy(errorRes = R.string.error_password_empty) }
            return
        }
        _state.update { it.copy(isDeleting = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = repository.deleteAccount(s.password)) {
                // The session flow returns the app to the login screen; just wipe this state.
                AuthResult.Success -> _state.value = ProfileUiState()
                is AuthResult.Failure -> _state.update {
                    it.copy(
                        isDeleting = false,
                        errorRes = if (result.error == AuthError.INVALID_CREDENTIALS) {
                            R.string.delete_error_wrong_password
                        } else {
                            result.error.messageRes()
                        },
                    )
                }
            }
        }
    }
}
