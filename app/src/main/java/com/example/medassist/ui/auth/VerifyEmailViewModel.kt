package com.example.medassist.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medassist.R
import com.example.medassist.data.model.Session
import com.example.medassist.data.repository.AuthRepository
import com.example.medassist.data.repository.AuthResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyEmailUiState(
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    /** Seconds until "Resend" works again. Stops people hammering the send limit. */
    val cooldownSeconds: Int = 0,
    @StringRes val infoRes: Int? = null,
    @StringRes val errorRes: Int? = null,
)

class VerifyEmailViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(VerifyEmailUiState())
    val state: StateFlow<VerifyEmailUiState> = _state.asStateFlow()

    private var cooldownJob: Job? = null

    /**
     * Asks Firebase if the email is now verified. If it is, the session flow switches to the main app
     * by itself. [manual] is true when the user tapped the button, so we tell them if nothing changed;
     * the automatic check when returning from the email app stays quiet.
     */
    fun check(manual: Boolean) {
        if (_state.value.isChecking) return
        _state.update { it.copy(isChecking = manual, infoRes = null, errorRes = null) }
        viewModelScope.launch {
            val result = repository.reloadUser()
            val stillUnverified = repository.session.value is Session.Unverified
            _state.update {
                when {
                    result is AuthResult.Failure && manual ->
                        it.copy(isChecking = false, errorRes = result.error.messageRes())
                    result is AuthResult.Success && stillUnverified && manual ->
                        it.copy(isChecking = false, infoRes = R.string.verify_not_yet)
                    else -> it.copy(isChecking = false)
                }
            }
        }
    }

    fun resend() {
        val s = _state.value
        if (s.isResending || s.cooldownSeconds > 0) return
        _state.update { it.copy(isResending = true, infoRes = null, errorRes = null) }
        viewModelScope.launch {
            when (val result = repository.sendVerificationEmail()) {
                AuthResult.Success -> {
                    _state.update { it.copy(isResending = false, infoRes = R.string.verify_resent) }
                    startCooldown()
                }
                is AuthResult.Failure -> _state.update {
                    it.copy(isResending = false, errorRes = result.error.messageRes())
                }
            }
        }
    }

    fun signOut() {
        cooldownJob?.cancel()
        _state.value = VerifyEmailUiState()
        repository.signOut()
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
                _state.update { it.copy(cooldownSeconds = remaining) }
                delay(1_000)
            }
            _state.update { it.copy(cooldownSeconds = 0) }
        }
    }

    private companion object {
        const val RESEND_COOLDOWN_SECONDS = 60
    }
}
