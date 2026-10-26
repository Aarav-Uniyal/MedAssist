package com.example.medassist

import android.app.Application

/**
 * Application entry point. Creates the [AppContainer] once so every screen shares the same objects.
 */
class MedAssistApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Must run before any Firebase AI Logic request (see AppCheckInstaller.kt in src/debug and src/release).
        installAppCheck()
        container = AppContainer(this)
    }
}
