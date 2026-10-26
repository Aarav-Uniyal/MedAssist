package com.example.medassist.data.repository

import android.util.Log
import com.example.medassist.data.model.AuthUser
import com.example.medassist.data.model.Session
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "MedAssistAuth"

/**
 * Firebase-backed [AuthRepository].
 *
 * Passwords go straight to Firebase Authentication, which stores them securely. They are never
 * written to Firestore or to disk by this app.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    /** Deletes everything the user stored under users/{uid} (the Medical Vault). Throws on failure. */
    private val deleteUserData: suspend (uid: String) -> Unit = {},
    /** Wipes files this app keeps on the phone for the signed-in user (downloaded PDFs). */
    private val clearLocalData: () -> Unit = {},
) : AuthRepository {

    private val _session = MutableStateFlow<Session>(Session.Unknown)
    override val session: StateFlow<Session> = _session.asStateFlow()

    init {
        // Called once right away with the saved login (if any), then on every sign-in or sign-out.
        auth.addAuthStateListener { firebaseAuth ->
            _session.value = firebaseAuth.currentUser.toSession()
        }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = runAuth {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun register(name: String, email: String, password: String): AuthResult {
        val created = runAuth {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: error("Account was created but no user was returned")
            // The account exists now. The two steps below are nice-to-have, so a failure in them
            // must not make registration look like it failed. The verification screen has a Resend
            // button, and the profile document is recreated by ensureProfile() if it is missing.
            runCatching {
                user.updateProfile(userProfileChangeRequest { displayName = name }).await()
                user.reload().await()
                _session.value = auth.currentUser.toSession()
            }.onFailure { Log.w(TAG, "Saving display name failed", it) }
            runCatching {
                auth.useAppLanguage()
                user.sendEmailVerification().await()
            }.onFailure { Log.w(TAG, "Sending verification email failed", it) }
        }
        return created
    }

    override suspend fun sendPasswordReset(email: String): AuthResult = runAuth {
        // Send the email in the language the phone is using.
        auth.useAppLanguage()
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun sendVerificationEmail(): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Failure(AuthError.SESSION_EXPIRED)
        return runAuth {
            auth.useAppLanguage()
            user.sendEmailVerification().await()
        }
    }

    override suspend fun reloadUser(): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Failure(AuthError.SESSION_EXPIRED)
        return try {
            user.reload().await()
            if (user.isEmailVerified) {
                // Security rules read the "email_verified" claim from the login token, so fetch
                // a fresh token now instead of waiting up to an hour for the old one to expire.
                user.getIdToken(true).await()
            }
            // reload() does not notify the auth-state listener, so publish the new state ourselves.
            _session.value = auth.currentUser.toSession()
            AuthResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            // The account was deleted or disabled elsewhere. Do not stay "signed in" to nothing.
            auth.signOut()
            AuthResult.Failure(AuthError.SESSION_EXPIRED)
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthError())
        }
    }

    override suspend fun ensureProfile(): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Failure(AuthError.SESSION_EXPIRED)
        return runAuth {
            val ref = firestore.collection("users").document(user.uid)
            if (!ref.get().await().exists()) {
                // Only fields the app needs. Field names and types must match firestore.rules.
                ref.set(
                    mapOf(
                        "displayName" to user.displayName.orEmpty(),
                        "email" to user.email.orEmpty(),
                        "preferredLanguage" to "en",
                        "narrationEnabled" to false,
                        "dailyStepGoal" to 8000,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }
        }
    }

    override fun signOut() {
        clearLocalData()
        auth.signOut()
    }

    override suspend fun deleteAccount(password: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Failure(AuthError.SESSION_EXPIRED)
        val email = user.email ?: return AuthResult.Failure(AuthError.UNKNOWN)
        return runAuth {
            // Deleting an account is a sensitive action, so Firebase insists on a recent login.
            user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
            // Data first, account last: if a data step fails the user still has an account and
            // can simply try again. This runs in the app (not a Cloud Function) because Cloud
            // Functions need the paid Blaze plan; the security rules let only the owner delete.
            deleteUserData(user.uid)
            firestore.collection("users").document(user.uid).delete().await()
            clearLocalData()
            user.delete().await()
            // The auth-state listener now reports SignedOut and the app returns to the login screen.
        }
    }

    /** Runs [block], turning any failure into an [AuthResult.Failure]. */
    private suspend inline fun runAuth(block: () -> Unit): AuthResult = try {
        block()
        AuthResult.Success
    } catch (e: CancellationException) {
        throw e // never swallow cancellation, or coroutines cannot stop
    } catch (e: Exception) {
        Log.w(TAG, "Auth operation failed", e)
        AuthResult.Failure(e.toAuthError())
    }

    private fun FirebaseUser?.toSession(): Session = when {
        this == null -> Session.SignedOut
        !isEmailVerified -> Session.Unverified(email.orEmpty())
        else -> Session.Verified(AuthUser(uid = uid, displayName = displayName.orEmpty(), email = email.orEmpty()))
    }
}
