package com.example.medassist.domain

import com.example.medassist.domain.vault.ReportTextPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTextPolicyTest {
    @Test
    fun prepare_normalizesWhitespaceButKeepsLinesReadable() {
        val result = ReportTextPolicy.prepare("  HbA1c:   5.7 %\r\n\r\nReference range:  4.0–5.6 %  ")

        requireNotNull(result)
        assertEquals("HbA1c: 5.7 %\n\nReference range: 4.0–5.6 %", result.text)
        assertFalse(result.truncated)
    }

    @Test
    fun prepare_rejectsTooLittleSelectableText() {
        assertNull(ReportTextPolicy.prepare("   scanned page \n "))
    }

    @Test
    fun prepare_capsLongTextAndMarksItTruncated() {
        val raw = "A".repeat(ReportTextPolicy.MAX_ANALYSIS_CHARS + 100)
        val result = ReportTextPolicy.prepare(raw)

        requireNotNull(result)
        assertEquals(ReportTextPolicy.MAX_ANALYSIS_CHARS, result.text.length)
        assertTrue(result.truncated)
    }
}
