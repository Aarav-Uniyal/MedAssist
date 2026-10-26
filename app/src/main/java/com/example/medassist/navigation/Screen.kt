package com.example.medassist.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.medassist.R

/** Every destination in the app and its route name. */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Assistant : Screen("assistant")
    data object Vault : Screen("vault")
    data object Steps : Screen("steps")
    data object Profile : Screen("profile")

    /** Report Analyzer is available from Home and as a dedicated bottom-navigation tab. */
    data object Analyzer : Screen("analyzer")

    /** PDF viewer for one vault document. */
    data object Document : Screen("document/{documentId}") {
        const val ARG_ID = "documentId"
        fun route(documentId: String) = "document/$documentId"
    }

    // Signed-out screens. They live in their own NavHost (see ui/auth/AuthFlow.kt).
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
}

/** One entry in the bottom bar. Labels are short so they fit on a small phone. */
data class BottomNavItem(
    val screen: Screen,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, R.string.nav_home, R.drawable.ic_home),
    BottomNavItem(Screen.Vault, R.string.nav_vault, R.drawable.ic_folder),
    BottomNavItem(Screen.Assistant, R.string.nav_assistant, R.drawable.ic_chat),
    BottomNavItem(Screen.Analyzer, R.string.nav_analyzer, R.drawable.ic_report),
    BottomNavItem(Screen.Profile, R.string.nav_profile, R.drawable.ic_person),
)
