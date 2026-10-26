package com.example.medassist.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException

/**
 * Turns a Firebase error code into an [AuthError]. Kept as a plain function of a String so it
 * can be unit-tested without Firebase.
 *
 * Wrong password and unknown email deliberately give the same error. Telling them apart would let
 * a stranger find out which email addresses have accounts.
 */
fun authErrorForCode(code: String?): AuthError = when (code) {
    "ERROR_INVALID_CREDENTIAL",
    "ERROR_WRONG_PASSWORD",
    "ERROR_USER_NOT_FOUND",
    -> AuthError.INVALID_CREDENTIALS

    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthError.EMAIL_IN_USE
    "ERROR_WEAK_PASSWORD",
    "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS",
    -> AuthError.WEAK_PASSWORD

    "ERROR_INVALID_EMAIL" -> AuthError.INVALID_EMAIL
    "ERROR_USER_DISABLED" -> AuthError.USER_DISABLED
    "ERROR_USER_TOKEN_EXPIRED",
    "ERROR_INVALID_USER_TOKEN",
    "ERROR_REQUIRES_RECENT_LOGIN",
    -> AuthError.SESSION_EXPIRED

    "ERROR_TOO_MANY_REQUESTS" -> AuthError.TOO_MANY_REQUESTS
    "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NETWORK
    else -> AuthError.UNKNOWN
}

fun Throwable.toAuthError(): AuthError = when (this) {
    is FirebaseNetworkException -> AuthError.NETWORK
    is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
    is FirebaseAuthException -> authErrorForCode(errorCode)
    else -> AuthError.UNKNOWN
}
