package com.example.medassist.domain.vault

import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.DocumentSort
import com.example.medassist.data.model.MedicalDocument
import java.util.Locale

/** Search, filter and sort the vault list on the phone (a user has few enough documents for that). */
object DocumentQuery {
    fun apply(
        documents: List<MedicalDocument>,
        search: String,
        category: DocumentCategory?,
        sort: DocumentSort,
        locale: Locale = Locale.getDefault(),
    ): List<MedicalDocument> {
        val needle = search.trim().lowercase(locale)
        val filtered = documents.filter { doc ->
            (category == null || doc.category == category) &&
                (needle.isEmpty() || doc.name.lowercase(locale).contains(needle) || doc.notes.lowercase(locale).contains(needle))
        }
        return when (sort) {
            DocumentSort.NEWEST -> filtered.sortedByDescending { it.uploadedAtMillis }
            DocumentSort.OLDEST -> filtered.sortedBy { it.uploadedAtMillis }
            DocumentSort.NAME -> filtered.sortedBy { it.name.lowercase(locale) }
            DocumentSort.LARGEST -> filtered.sortedByDescending { it.sizeBytes }
        }
    }
}
