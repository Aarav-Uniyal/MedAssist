package com.example.medassist.ui.auth

import androidx.annotation.StringRes
import com.example.medassist.R
import com.example.medassist.data.repository.AuthError
import com.example.medassist.domain.validation.FieldError

/** Maps validation and Firebase errors to localized message resources (ViewModels stay Context-free). */
@StringRes
fun FieldError.messageRes(): Int = when (this) {
    FieldError.NAME_EMPTY -> R.string.error_name_empty
    FieldError.NAME_TOO_LONG -> R.string.error_name_too_long
    FieldError.EMAIL_EMPTY -> R.string.error_email_empty
    FieldError.EMAIL_INVALID -> R.string.error_email_invalid
    FieldError.PASSWORD_EMPTY -> R.string.error_password_empty
    FieldError.PASSWORD_TOO_SHORT -> R.string.error_password_short
    FieldError.PASSWORD_WEAK -> R.string.error_password_weak
    FieldError.CONFIRM_EMPTY -> R.string.error_confirm_empty
    FieldError.PASSWORDS_MISMATCH -> R.string.error_passwords_mismatch
}

@StringRes
fun AuthError.messageRes(): Int = when (this) {
    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
    AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
    AuthError.INVALID_EMAIL -> R.string.error_email_invalid
    AuthError.NETWORK -> R.string.auth_error_network
    AuthError.TOO_MANY_REQUESTS -> R.string.auth_error_too_many
    AuthError.USER_DISABLED -> R.string.auth_error_disabled
    AuthError.SESSION_EXPIRED -> R.string.auth_error_session_expired
    AuthError.UNKNOWN -> R.string.auth_error_unknown
}
