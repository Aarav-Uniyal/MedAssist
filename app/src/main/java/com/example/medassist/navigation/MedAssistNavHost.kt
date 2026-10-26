package com.example.medassist.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.medassist.ui.common.containerViewModelFactory
import com.example.medassist.ui.home.RecentDocumentsViewModel
import com.example.medassist.ui.vault.DocumentViewerScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.medassist.data.model.AuthUser
import com.example.medassist.ui.analyzer.AnalyzerScreen
import com.example.medassist.ui.analyzer.ReportAnalyzerViewModel
import com.example.medassist.ui.chatbot.ChatViewModel
import com.example.medassist.ui.chatbot.ChatbotScreen
import com.example.medassist.ui.home.HomeScreen
import com.example.medassist.ui.settings.ProfileScreen
import com.example.medassist.ui.steps.StepsScreen
import com.example.medassist.ui.vault.VaultScreen
import com.example.medassist.ui.theme.MedAssistTheme

/** Maps each route to its screen. Screens receive plain callbacks, never the NavController. */
@Composable
fun MedAssistNavHost(
    navController: NavHostController,
    user: AuthUser,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        // Short fades only: motion should be restrained.
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Screen.Home.route) {
            val recentVm: RecentDocumentsViewModel = viewModel(
                key = "recent:${user.uid}",
                factory = containerViewModelFactory { RecentDocumentsViewModel(it.vaultRepository) },
            )
            val recent by recentVm.recent.collectAsStateWithLifecycle()
            HomeScreen(
                userName = user.displayName,
                onOpenTab = { screen -> navController.navigateToTab(screen.route) },
                onOpenAnalyzer = { navController.navigateToTab(Screen.Analyzer.route) },
                recentDocuments = recent,
                onOpenDocument = { id -> navController.navigate(Screen.Document.route(id)) },
            )
        }
        composable(Screen.Assistant.route) {
            val chatVm: ChatViewModel = viewModel(
                key = "chat:${user.uid}",
                factory = containerViewModelFactory {
                    ChatViewModel(
                        uid = user.uid,
                        repository = it.assistantRepository,
                        historyStore = it.chatHistoryStore,
                        isOnline = it.connectivityObserver.isOnline,
                        language = it.currentLanguage,
                    )
                },
            )
            ChatbotScreen(chatVm)
        }
        composable(Screen.Vault.route) {
            VaultScreen(
                uid = user.uid,
                onOpenDocument = { id -> navController.navigate(Screen.Document.route(id)) },
            )
        }
        composable(
            Screen.Document.route,
            arguments = listOf(navArgument(Screen.Document.ARG_ID) { type = NavType.StringType }),
        ) { entry ->
            MedAssistTheme(darkTheme = false) {
                DocumentViewerScreen(
                    uid = user.uid,
                    documentId = entry.arguments?.getString(Screen.Document.ARG_ID).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
        composable(Screen.Steps.route) { StepsScreen() }
        composable(Screen.Profile.route) { ProfileScreen(user = user) }
        composable(Screen.Analyzer.route) {
            val analyzerVm: ReportAnalyzerViewModel = viewModel(
                key = "analyzer:${user.uid}",
                factory = containerViewModelFactory {
                    ReportAnalyzerViewModel(
                        uid = user.uid,
                        vaultRepository = it.vaultRepository,
                        documentCache = it.documentCache,
                        pdfTextExtractor = it.pdfTextExtractor,
                        analyzerRepository = it.reportAnalyzerRepository,
                        isOnline = it.connectivityObserver.isOnline,
                        language = it.currentLanguage,
                    )
                },
            )
            AnalyzerScreen(
                viewModel = analyzerVm,
                onOpenVault = { navController.navigateToTab(Screen.Vault.route) },
                onOpenDocument = { id -> navController.navigate(Screen.Document.route(id)) },
            )
        }
    }
}

/**
 * Switch bottom-bar tabs the standard way: keep one copy of each tab, remember its state,
 * and return to Home when Back is pressed.
 */
fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
