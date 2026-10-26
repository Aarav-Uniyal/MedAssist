package com.example.medassist

import android.content.Context
import com.example.medassist.data.local.AndroidPdfPageSource
import com.example.medassist.data.local.AppNarrator
import com.example.medassist.data.local.AppPreferences
import com.example.medassist.data.local.AndroidPickedPdfReader
import com.example.medassist.data.local.ChatHistoryStore
import com.example.medassist.data.local.FileChatHistoryStore
import com.example.medassist.data.local.DocumentCache
import com.example.medassist.data.local.PdfPageSourceFactory
import com.example.medassist.data.local.PickedPdfReader
import com.example.medassist.data.local.PdfTextExtractor
import com.example.medassist.data.local.AndroidPdfTextExtractor
import com.example.medassist.data.repository.AssistantRepository
import com.example.medassist.data.repository.AssistantSettings
import com.example.medassist.data.repository.AuthRepository
import com.example.medassist.data.repository.FirebaseAssistantRepository
import com.example.medassist.data.repository.FirebaseReportAnalyzerRepository
import com.example.medassist.data.repository.ReportAnalyzerRepository
import com.example.medassist.data.repository.FirebaseAuthRepository
import com.example.medassist.data.repository.FirestoreVaultRepository
import com.example.medassist.data.repository.VaultRepository
import com.example.medassist.domain.assistant.AppLanguage
import com.example.medassist.utils.ConnectivityObserver
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A very small "dependency container": one place that creates shared objects.
 * Written by hand on purpose, so there is no Hilt/Dagger to learn.
 */
class AppContainer(context: Context) {
    val appPreferences = AppPreferences(context)
    val narrator = AppNarrator(context)
    val connectivityObserver = ConnectivityObserver(context)

    val documentCache = DocumentCache(context)
    val pickedPdfReader: PickedPdfReader = AndroidPickedPdfReader(context)
    val pdfPageSourceFactory = PdfPageSourceFactory { file -> AndroidPdfPageSource(file) }
    val pdfTextExtractor: PdfTextExtractor by lazy { AndroidPdfTextExtractor(context) }

    val chatHistoryStore: ChatHistoryStore = FileChatHistoryStore(context)

    /** The app's current language (the per-app setting if one is chosen, otherwise the phone's). */
    val currentLanguage: () -> AppLanguage = {
        AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language)
    }

    private val assistantSettings: AssistantSettings by lazy {
        AssistantSettings(FirebaseRemoteConfig.getInstance())
    }

    val assistantRepository: AssistantRepository by lazy {
        FirebaseAssistantRepository(modelName = assistantSettings::modelName)
    }

    /** Phase 5 shares the same free-tier model setting as the chatbot. */
    val reportAnalyzerRepository: ReportAnalyzerRepository by lazy {
        FirebaseReportAnalyzerRepository(modelName = assistantSettings::modelName)
    }

    // "by lazy" means Firebase is only touched when something first needs it.
    val vaultRepository: VaultRepository by lazy {
        FirestoreVaultRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
        )
    }

    val authRepository: AuthRepository by lazy {
        FirebaseAuthRepository(
            auth = FirebaseAuth.getInstance(),
            firestore = FirebaseFirestore.getInstance(),
            deleteUserData = vaultRepository::deleteAllUserData,
            clearLocalData = {
                documentCache.clearAll()
                chatHistoryStore.clearAll()
            },
        )
    }
}
