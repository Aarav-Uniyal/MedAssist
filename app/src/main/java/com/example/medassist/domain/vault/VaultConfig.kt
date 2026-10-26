package com.example.medassist.domain.vault

/**
 * Limits for the Medical Vault. The same numbers appear in firestore.rules; change both together.
 */
object VaultConfig {
    /** Largest PDF a user may upload (10 MB). Raise it carefully: every MB counts against the free 1 GB. */
    const val MAX_FILE_BYTES: Long = 10L * 1024 * 1024

    /**
     * PDFs are stored as pieces ("chunks") because a Firestore document can hold at most 1 MiB.
     * 900 KiB leaves room for the other fields in the chunk document.
     */
    const val CHUNK_BYTES: Int = 900 * 1024

    const val NAME_MAX_LENGTH = 100
    const val NOTES_MAX_LENGTH = 500

    const val MIME_PDF = "application/pdf"
}
