package com.example.medassist

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medassist.ui.MedAssistApp
import com.example.medassist.ui.theme.LocalAppNarrator
import com.example.medassist.ui.theme.LocalAppPreferences
import com.example.medassist.ui.theme.MedAssistTheme

/** Single activity. App-language changes are managed by AppCompatDelegate. */
class MainActivity : AppCompatActivity() {

    private lateinit var appContainer: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContainer = (application as MedAssistApplication).container
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )

        setContent {
            val settings by appContainer.appPreferences.settings.collectAsStateWithLifecycle()
            SideEffect {
                // Keep transparent edge-to-edge bars but switch their icons for contrast.
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !settings.darkMode
                controller.isAppearanceLightNavigationBars = !settings.darkMode
            }
            DisposableEffect(appContainer.narrator) {
                onDispose { appContainer.narrator.stop() }
            }
            MedAssistTheme(darkTheme = settings.darkMode) {
                CompositionLocalProvider(
                    LocalAppPreferences provides appContainer.appPreferences,
                    LocalAppNarrator provides appContainer.narrator,
                ) {
                    MedAssistApp(isOnline = appContainer.connectivityObserver.isOnline)
                }
            }
        }
    }

    override fun onDestroy() {
        if (::appContainer.isInitialized) appContainer.narrator.release()
        super.onDestroy()
    }
}
