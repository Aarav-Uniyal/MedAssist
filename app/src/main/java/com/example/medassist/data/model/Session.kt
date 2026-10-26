package com.example.medassist.data.model

/** The signed-in person, as the UI needs to know them. No password or token ever lives here. */
data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
)

/**
 * Where the user is in the sign-in journey. The root of the app shows a different screen for each,
 * so the main screens are simply never created unless the state is [Verified].
 */
sealed interface Session {
    /** Firebase has not told us yet (first moments after launch). */
    data object Unknown : Session

    data object SignedOut : Session

    /** Signed in, but the email address has not been confirmed. */
    data class Unverified(val email: String) : Session

    data class Verified(val user: AuthUser) : Session
}
