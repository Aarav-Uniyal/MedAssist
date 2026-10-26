package com.example.medassist.domain.validation

/** What is wrong with a form field. The UI turns each value into a localized message. */
enum class FieldError {
    NAME_EMPTY,
    NAME_TOO_LONG,
    EMAIL_EMPTY,
    EMAIL_INVALID,
    PASSWORD_EMPTY,
    PASSWORD_TOO_SHORT,
    PASSWORD_WEAK,
    CONFIRM_EMPTY,
    PASSWORDS_MISMATCH,
}

/**
 * Checks form input before anything is sent to Firebase. Plain Kotlin with no Android classes,
 * so it is covered by ordinary JVM unit tests. Each function returns null when the input is fine.
 */
object AuthValidator {
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128
    const val MAX_NAME_LENGTH = 60

    // Deliberately simple: something@something.tld with no spaces. Firebase does the final check.
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun validateName(name: String): FieldError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> FieldError.NAME_EMPTY
            trimmed.length > MAX_NAME_LENGTH -> FieldError.NAME_TOO_LONG
            else -> null
        }
    }

    fun validateEmail(email: String): FieldError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> FieldError.EMAIL_EMPTY
            !emailPattern.matches(trimmed) -> FieldError.EMAIL_INVALID
            else -> null
        }
    }

    /** For signing in we only require that something was typed; strength rules apply at sign-up. */
    fun validatePasswordPresent(password: String): FieldError? =
        if (password.isEmpty()) FieldError.PASSWORD_EMPTY else null

    /** Sign-up rule: at least 8 characters, with at least one letter and one digit. */
    fun validateNewPassword(password: String): FieldError? = when {
        password.isEmpty() -> FieldError.PASSWORD_EMPTY
        password.length < MIN_PASSWORD_LENGTH -> FieldError.PASSWORD_TOO_SHORT
        password.length > MAX_PASSWORD_LENGTH -> FieldError.PASSWORD_WEAK
        password.none { it.isLetter() } || password.none { it.isDigit() } -> FieldError.PASSWORD_WEAK
        else -> null
    }

    fun validateConfirm(password: String, confirm: String): FieldError? = when {
        confirm.isEmpty() -> FieldError.CONFIRM_EMPTY
        confirm != password -> FieldError.PASSWORDS_MISMATCH
        else -> null
    }
}
