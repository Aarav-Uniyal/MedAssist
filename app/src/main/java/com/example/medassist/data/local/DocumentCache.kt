package com.example.medassist.data.local

import android.content.Context
import java.io.File

/**
 * Downloaded PDFs live in the app's private cache (no other app can read it, and Android may clear it).
 * Files are named by document id and kept in a folder per user, so switching accounts never shows
 * someone else's file. Everything is wiped on sign-out and account deletion.
 */
class DocumentCache(context: Context) {
    private val root = File(context.applicationContext.cacheDir, "vault")

    fun fileFor(uid: String, documentId: String): File = File(File(root, safe(uid)), "${safe(documentId)}.pdf")

    fun remove(uid: String, documentId: String) {
        fileFor(uid, documentId).delete()
    }

    fun clearAll() {
        root.deleteRecursively()
    }

    // Firebase ids are letters and digits; this just makes sure no id can point outside the folder.
    private fun safe(id: String) = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
