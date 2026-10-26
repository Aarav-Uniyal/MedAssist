package com.example.medassist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.data.model.Session
import com.example.medassist.ui.auth.AuthFlow
import com.example.medassist.ui.auth.SessionViewModel
import com.example.medassist.ui.auth.VerifyEmailScreen
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.components.LoadingState
import kotlinx.coroutines.flow.Flow

/**
 * The root of the app. It shows exactly one thing, chosen by the sign-in state:
 *
 *   Unknown     -> a loading spinner (Firebase is reading the saved login)
 *   SignedOut   -> login / register / forgot password
 *   Unverified  -> "verify your email"
 *   Verified    -> the real app (MainShell)
 *
 * This is how screens are protected: the main screens are not merely hidden, they are never
 * created unless the user is signed in and verified. Signing out removes them again.
 */
@Composable
fun MedAssistApp(isOnline: Flow<Boolean>) {
    val sessionViewModel: SessionViewModel = viewModel(
        factory = containerViewModelFactory { SessionViewModel(it.authRepository) },
    )
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val online by isOnline.collectAsStateWithLifecycle(initialValue = true)

    when (val current = session) {
        Session.Unknown -> Box(Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            LoadingState(Modifier.fillMaxSize())
        }
        Session.SignedOut -> AuthFlow(offline = !online)
        is Session.Unverified -> VerifyEmailScreen(email = current.email, offline = !online)
        is Session.Verified -> MainShell(isOnline = isOnline, user = current.user)
    }
}
