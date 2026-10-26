package com.example.medassist.testutil

import com.example.medassist.data.model.Session
import com.example.medassist.data.repository.AuthRepository
import com.example.medassist.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A stand-in for Firebase: records what was asked and returns whatever result the test sets. */
class FakeAuthRepository : AuthRepository {
    override val session: StateFlow<Session> = MutableStateFlow(Session.SignedOut)

    var nextResult: AuthResult = AuthResult.Success

    var signInCalls = mutableListOf<Pair<String, String>>()
    var registerCalls = mutableListOf<Triple<String, String, String>>()
    var resetCalls = mutableListOf<String>()
    var deleteCalls = mutableListOf<String>()
    var signOutCalls = 0

    override suspend fun signIn(email: String, password: String): AuthResult {
        signInCalls += email to password
        return nextResult
    }

    override suspend fun register(name: String, email: String, password: String): AuthResult {
        registerCalls += Triple(name, email, password)
        return nextResult
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        resetCalls += email
        return nextResult
    }

    override suspend fun sendVerificationEmail(): AuthResult = nextResult
    override suspend fun reloadUser(): AuthResult = nextResult
    override suspend fun ensureProfile(): AuthResult = nextResult

    override fun signOut() {
        signOutCalls++
    }

    override suspend fun deleteAccount(password: String): AuthResult {
        deleteCalls += password
        return nextResult
    }
}
