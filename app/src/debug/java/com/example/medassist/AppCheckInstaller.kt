package com.example.medassist

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds only. On first run Logcat (tag "DebugAppCheckProvider") prints a debug token. Add it in the
 * Firebase console under Security > App Check > Apps > Manage debug tokens. Never commit or share that token.
 */
internal fun installAppCheck() {
    Firebase.appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    // Fetch a token once at start-up so Logcat shows straight away whether the debug token is registered.
    Firebase.appCheck.getAppCheckToken(false)
        .addOnSuccessListener { Log.i(TAG, "App Check OK: debug token is registered") }
        .addOnFailureListener { Log.w(TAG, "App Check FAILED (register the debug token in the console): ${it.message}") }
}

private const val TAG = "MedAssistAppCheck"
