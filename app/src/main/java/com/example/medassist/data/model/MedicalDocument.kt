package com.example.medassist.data.model

/**
 * Categories a user can give a document. [key] is what is stored in Firestore (and checked by
 * firestore.rules), so never rename a key once documents exist.
 */
enum class DocumentCategory(val key: String) {
    BLOOD_TESTS("blood_tests"),
    PRESCRIPTIONS("prescriptions"),
    DIAGNOSTIC_REPORTS("diagnostic_reports"),
    IMAGING_REPORTS("imaging_reports"),
    MEDICAL_HISTORY("medical_history"),
    OTHER("other"),
    ;

    companion object {
        fun fromKey(key: String?): DocumentCategory = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** One PDF in the Medical Vault, as the UI sees it (metadata only, never the file bytes). */
data class MedicalDocument(
    val id: String,
    val name: String,
    val category: DocumentCategory,
    val notes: String,
    val sizeBytes: Long,
    val uploadedAtMillis: Long,
    val chunkCount: Int,
    val sha256: String,
)

/** Ways the vault list can be ordered. */
enum class DocumentSort { NEWEST, OLDEST, NAME, LARGEST }
