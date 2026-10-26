package com.example.medassist.domain

import com.example.medassist.data.model.DocumentCategory
import com.example.medassist.data.model.DocumentSort
import com.example.medassist.domain.vault.DocumentQuery
import com.example.medassist.testutil.FakeVaultRepository.Companion.doc
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DocumentQueryTest {
    private val docs = listOf(
        doc("1", "Blood test March", DocumentCategory.BLOOD_TESTS, uploadedAt = 300, sizeBytes = 10),
        doc("2", "x-ray chest", DocumentCategory.IMAGING_REPORTS, uploadedAt = 100, sizeBytes = 30, notes = "Dr. Rao"),
        doc("3", "Antibiotics", DocumentCategory.PRESCRIPTIONS, uploadedAt = 200, sizeBytes = 20),
    )

    private fun ids(search: String = "", cat: DocumentCategory? = null, sort: DocumentSort = DocumentSort.NEWEST) =
        DocumentQuery.apply(docs, search, cat, sort, Locale.ENGLISH).map { it.id }

    @Test fun sorts() {
        assertEquals(listOf("1", "3", "2"), ids())
        assertEquals(listOf("2", "3", "1"), ids(sort = DocumentSort.OLDEST))
        assertEquals(listOf("3", "1", "2"), ids(sort = DocumentSort.NAME))
        assertEquals(listOf("2", "3", "1"), ids(sort = DocumentSort.LARGEST))
    }

    @Test fun search_isCaseInsensitive_andCoversNotes() {
        assertEquals(listOf("1"), ids(search = "  BLOOD "))
        assertEquals(listOf("2"), ids(search = "rao"))
        assertEquals(emptyList<String>(), ids(search = "zzz"))
    }

    @Test fun categoryFilter() = assertEquals(listOf("3"), ids(cat = DocumentCategory.PRESCRIPTIONS))
}
