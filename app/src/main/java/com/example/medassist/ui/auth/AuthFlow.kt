package com.example.medassist.ui.auth

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medassist.navigation.Screen

/** The screens a signed-out user can reach. Kept separate from the main app's navigation. */
@Composable
fun AuthFlow(offline: Boolean) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                offline = offline,
                onForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                onRegister = { navController.navigate(Screen.Register.route) },
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(offline = offline, onBackToLogin = { navController.popBackStack() })
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(offline = offline, onBackToLogin = { navController.popBackStack() })
        }
    }
}
