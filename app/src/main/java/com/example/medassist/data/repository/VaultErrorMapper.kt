package com.example.medassist.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/** Turns a Firestore error code name into a [VaultError]. A plain function of a String, so it is unit-tested. */
fun vaultErrorForCode(code: String?): VaultError = when (code) {
    "UNAVAILABLE", "DEADLINE_EXCEEDED" -> VaultError.NETWORK
    "UNAUTHENTICATED" -> VaultError.SESSION_EXPIRED
    "PERMISSION_DENIED" -> VaultError.PERMISSION_DENIED
    "NOT_FOUND" -> VaultError.NOT_FOUND
    // On the free Spark plan this means the daily Firestore quota is used up (it resets daily).
    "RESOURCE_EXHAUSTED" -> VaultError.QUOTA_EXCEEDED
    "DATA_LOSS" -> VaultError.CORRUPTED
    else -> VaultError.UNKNOWN
}

fun Throwable.toVaultError(): VaultError = when (this) {
    is FirebaseFirestoreException -> vaultErrorForCode(code.name)
    is FirebaseNetworkException -> VaultError.NETWORK
    is IOException -> if (message.orEmpty().contains("ENOSPC")) VaultError.DEVICE_STORAGE_FULL else VaultError.UNREADABLE
    else -> VaultError.UNKNOWN
}
