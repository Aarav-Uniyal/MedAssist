package com.example.medassist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.medassist.R
import com.example.medassist.data.model.AuthUser
import com.example.medassist.navigation.MedAssistNavHost
import com.example.medassist.navigation.Screen
import com.example.medassist.navigation.bottomNavItems
import com.example.medassist.navigation.navigateToTab
import com.example.medassist.ui.components.OfflineBanner
import com.example.medassist.ui.theme.Spacing
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import kotlinx.coroutines.flow.Flow
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/**
 * The app shell for signed-in, verified users: bottom navigation + offline banner + the current screen.
 * [isOnline] is a Flow (not a Boolean) so tests can pass `flowOf(true)` or `flowOf(false)`.
 */
@Composable
fun MainShell(isOnline: Flow<Boolean>, user: AuthUser) {
    val navController = rememberNavController()
    val online by isOnline.collectAsStateWithLifecycle(initialValue = true)
    val appPreferences = LocalAppPreferences.current
    val settings by appPreferences.settings.collectAsStateWithLifecycle()
    val narrator = LocalAppNarrator.current

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val screenNarration = when (currentRoute) {
        Screen.Home.route -> stringResource(R.string.a11y_speech_home)
        Screen.Vault.route -> stringResource(R.string.a11y_speech_vault)
        Screen.Assistant.route -> stringResource(R.string.a11y_speech_assistant)
        Screen.Analyzer.route -> stringResource(R.string.a11y_speech_analyzer)
        Screen.Profile.route -> stringResource(R.string.a11y_speech_profile)
        Screen.Document.route -> stringResource(R.string.a11y_speech_document)
        else -> ""
    }
    LaunchedEffect(currentRoute, settings.autoNarration, settings.narrationEnabled) {
        // Stop speech from the previous screen; optionally announce the new screen once.
        narrator.stop()
        if (settings.narrationEnabled && settings.autoNarration && screenNarration.isNotBlank()) {
            val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                .ifBlank { Locale.getDefault().toLanguageTag() }
            narrator.speak(screenNarration, tag, settings.narrationSpeed, settings.narrationVolume)
        }
    }
    // Only the five primary destinations in bottomNavItems display the bottom bar.
    val showBottomBar = bottomNavItems.any { it.screen.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                MedAssistBottomBar(navController = navController)
            }
        },
    ) { innerPadding ->
        // consumeWindowInsets stops each screen's own Scaffold from adding the same padding twice.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            OfflineBanner(visible = !online)
            NarrationPlaybackBar()
            MedAssistNavHost(
                navController = navController,
                user = user,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NarrationPlaybackBar() {
    val narrator = LocalAppNarrator.current
    val state by narrator.state.collectAsStateWithLifecycle()
    if (!state.isSpeaking && !state.isPaused) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(
            text = stringResource(if (state.isPaused) R.string.a11y_narration_paused else R.string.a11y_narration_playing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = narrator::pause,
                enabled = state.isSpeaking,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.settings_narration_pause)) }
            TextButton(
                onClick = narrator::resume,
                enabled = state.isPaused,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.settings_narration_resume)) }
            TextButton(
                onClick = narrator::stop,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.settings_narration_stop)) }
        }
    }
}

@Composable
private fun MedAssistBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                NavigationBarItem(
                    selected = selected,
                    onClick = { navController.navigateToTab(item.screen.route) },
                    // The label already names the tab, so the icon is decorative.
                    icon = { Icon(painter = painterResource(item.iconRes), contentDescription = null) },
                    label = {
                        Text(
                            text = stringResource(item.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    alwaysShowLabel = true,
                    // Selected = dark icon on an aqua pill. The pill shape (not only colour) shows selection.
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
