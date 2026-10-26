package com.example.medassist.domain

import com.example.medassist.domain.validation.DocumentFieldError
import com.example.medassist.domain.validation.DocumentValidator
import com.example.medassist.domain.validation.PdfProblem
import com.example.medassist.domain.vault.VaultConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentValidatorTest {
    private val pdfHeader = "%PDF-1.7\n".toByteArray()

    @Test fun validPdf_passes() = assertNull(DocumentValidator.checkPdf("application/pdf", 2000, pdfHeader))

    @Test fun emptyFile_rejected() = assertEquals(PdfProblem.EMPTY, DocumentValidator.checkPdf("application/pdf", 0, ByteArray(0)))

    @Test fun oversizedFile_rejected() = assertEquals(
        PdfProblem.TOO_LARGE,
        DocumentValidator.checkPdf("application/pdf", VaultConfig.MAX_FILE_BYTES + 1, pdfHeader),
    )

    @Test fun exactlyAtLimit_passes() = assertNull(DocumentValidator.checkPdf("application/pdf", VaultConfig.MAX_FILE_BYTES, pdfHeader))

    @Test fun limitIsConfigurable() = assertEquals(PdfProblem.TOO_LARGE, DocumentValidator.checkPdf("application/pdf", 2000, pdfHeader, maxBytes = 1000))

    @Test fun wrongMimeType_rejected() = assertEquals(PdfProblem.NOT_PDF, DocumentValidator.checkPdf("image/png", 2000, pdfHeader))

    @Test fun renamedNonPdf_rejectedByContent() = assertEquals(
        PdfProblem.NOT_PDF,
        DocumentValidator.checkPdf("application/pdf", 2000, "PK\u0003\u0004 zip file".toByteArray()),
    )

    @Test fun unknownMimeWithPdfContent_passes() = assertNull(DocumentValidator.checkPdf("application/octet-stream", 2000, pdfHeader))

    @Test fun headerAfterJunk_found() {
        assertTrue(DocumentValidator.hasPdfHeader(ByteArray(100) + pdfHeader))
        assertFalse(DocumentValidator.hasPdfHeader("%PD".toByteArray()))
    }

    @Test fun nameRules() {
        assertEquals(DocumentFieldError.NAME_EMPTY, DocumentValidator.validateName("   "))
        assertEquals(DocumentFieldError.NAME_TOO_LONG, DocumentValidator.validateName("a".repeat(101)))
        assertNull(DocumentValidator.validateName("  Blood test March  "))
    }

    @Test fun notesRules() {
        assertNull(DocumentValidator.validateNotes(""))
        assertEquals(DocumentFieldError.NOTES_TOO_LONG, DocumentValidator.validateNotes("a".repeat(501)))
    }

    @Test fun suggestedName_stripsExtension() {
        assertEquals("blood_report", DocumentValidator.suggestedName("blood_report.PDF"))
        assertEquals("", DocumentValidator.suggestedName(null))
        assertEquals(100, DocumentValidator.suggestedName("x".repeat(300) + ".pdf").length)
    }
}
