package com.example.medassist.data.repository

import com.example.medassist.data.model.Session
import kotlinx.coroutines.flow.StateFlow

/** Everything that can go wrong in the sign-in flows, in app terms (not Firebase terms). */
enum class AuthError {
    INVALID_CREDENTIALS,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    INVALID_EMAIL,
    NETWORK,
    TOO_MANY_REQUESTS,
    USER_DISABLED,
    SESSION_EXPIRED,
    UNKNOWN,
}

/** Result of an auth operation. A small sealed type keeps ViewModels free of try/catch. */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val error: AuthError) : AuthResult
}

/**
 * The only way the rest of the app talks to Firebase Authentication.
 * It is an interface so ViewModel tests can use a fake instead of the real thing.
 */
interface AuthRepository {
    /** Current sign-in state. Firebase keeps the login on the device, so this survives app restarts. */
    val session: StateFlow<Session>

    suspend fun signIn(email: String, password: String): AuthResult

    /** Creates the account, saves the name, and sends the verification email. */
    suspend fun register(name: String, email: String, password: String): AuthResult

    suspend fun sendPasswordReset(email: String): AuthResult

    suspend fun sendVerificationEmail(): AuthResult

    /** Asks Firebase whether the email has been verified since we last looked. */
    suspend fun reloadUser(): AuthResult

    /** Creates the user's Firestore profile document if it does not exist yet. */
    suspend fun ensureProfile(): AuthResult

    fun signOut()

    /** Re-checks the password, deletes the user's data, then the account itself. */
    suspend fun deleteAccount(password: String): AuthResult
}
