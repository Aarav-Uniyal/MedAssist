package com.example.medassist.data.repository

import android.util.Log
import com.example.medassist.domain.assistant.AssistantConfig
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Reads the assistant model from Firebase Remote Config.
 *
 * The in-app default makes the assistant usable before the first successful fetch. The first read
 * fetches and activates the published value, and a real-time listener activates later publications
 * while the app is in the foreground. Model IDs are validated by [AssistantConfig.resolveModelName].
 */
class AssistantSettings(private val remoteConfig: FirebaseRemoteConfig) {
    private val lock = Mutex()
    private var initialized = false
    private var updateRegistration: ConfigUpdateListenerRegistration? = null

    suspend fun modelName(): String {
        initializeIfNeeded()
        // Prefer the current key, but continue accepting the key used by earlier project versions.
        // Do not define an in-app default for the preferred key: an absent key must not mask the
        // published legacy value. resolveModelName() supplies the safe default for both cases.
        return AssistantConfig.resolveConfiguredModelName(
            preferredValue = remoteConfig.getString(AssistantConfig.PREFERRED_REMOTE_MODEL_KEY),
            legacyValue = remoteConfig.getString(AssistantConfig.REMOTE_MODEL_KEY),
        )
    }

    private suspend fun initializeIfNeeded() {
        lock.withLock {
            if (initialized) return

            try {
                // Await both operations so the default cannot race the initial fetch.
                remoteConfig.setConfigSettingsAsync(
                    remoteConfigSettings {
                        minimumFetchIntervalInSeconds = FETCH_INTERVAL_SECONDS
                    },
                ).await()
                remoteConfig.setDefaultsAsync(
                    mapOf(AssistantConfig.REMOTE_MODEL_KEY to AssistantConfig.DEFAULT_MODEL),
                ).await()

                val fetched = withTimeoutOrNull(INITIAL_FETCH_TIMEOUT_MILLIS) {
                    remoteConfig.fetchAndActivate().await()
                }
                if (fetched == null) {
                    Log.w(TAG, "Remote Config fetch timed out; using cached/default model.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Don't make configuration/network failures prevent the user from opening the chat.
                Log.w(TAG, "Remote Config initialization failed; using cached/default model.", e)
            }

            startRealtimeListener()
            initialized = true
        }
    }

    private fun startRealtimeListener() {
        if (updateRegistration != null) return

        try {
            updateRegistration = remoteConfig.addOnConfigUpdateListener(
                object : ConfigUpdateListener {
                    override fun onUpdate(configUpdate: ConfigUpdate) {
                        if (AssistantConfig.REMOTE_MODEL_KEY !in configUpdate.updatedKeys &&
                            AssistantConfig.PREFERRED_REMOTE_MODEL_KEY !in configUpdate.updatedKeys
                        ) return

                        remoteConfig.activate()
                            .addOnSuccessListener {
                                Log.d(TAG, "Published assistant model configuration activated.")
                            }
                            .addOnFailureListener { error ->
                                Log.w(TAG, "Could not activate assistant model configuration.", error)
                            }
                    }

                    override fun onError(error: FirebaseRemoteConfigException) {
                        Log.w(TAG, "Remote Config real-time listener failed.", error)
                    }
                },
            )
        } catch (e: Exception) {
            // The already-fetched/cached value remains usable if real-time listening is unavailable.
            Log.w(TAG, "Could not start Remote Config real-time listener.", e)
        }
    }

    private companion object {
        const val TAG = "MedAssistConfig"
        const val FETCH_INTERVAL_SECONDS = 3600L
        const val INITIAL_FETCH_TIMEOUT_MILLIS = 5_000L
    }
}
