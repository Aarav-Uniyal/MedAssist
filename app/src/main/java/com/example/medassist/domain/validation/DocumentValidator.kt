package com.example.medassist.domain.validation

import com.example.medassist.domain.vault.VaultConfig

/** Why a picked file cannot be uploaded. */
enum class PdfProblem { EMPTY, TOO_LARGE, NOT_PDF }

/** Problems with the name or notes a user typed for a document. */
enum class DocumentFieldError { NAME_EMPTY, NAME_TOO_LONG, NOTES_TOO_LONG }

/** Plain-Kotlin checks for the Medical Vault, unit-tested without a device. */
object DocumentValidator {

    private val PDF_MAGIC = "%PDF-".toByteArray(Charsets.US_ASCII)

    /** MIME types some file managers report for PDFs they cannot identify. The file header decides then. */
    private val UNKNOWN_MIME_TYPES = setOf(null, "", "application/octet-stream", "application/x-pdf")

    /**
     * Checks a picked file. [header] should be the first 1024 bytes (or the whole file if smaller).
     * The content is checked as well as the MIME type: a file renamed to .pdf is still rejected.
     */
    fun checkPdf(
        mimeType: String?,
        sizeBytes: Long,
        header: ByteArray,
        maxBytes: Long = VaultConfig.MAX_FILE_BYTES,
    ): PdfProblem? = when {
        sizeBytes <= 0L -> PdfProblem.EMPTY
        sizeBytes > maxBytes -> PdfProblem.TOO_LARGE
        mimeType != VaultConfig.MIME_PDF && mimeType !in UNKNOWN_MIME_TYPES -> PdfProblem.NOT_PDF
        !hasPdfHeader(header) -> PdfProblem.NOT_PDF
        else -> null
    }

    /** The PDF format allows a little junk before "%PDF-", so look in the first 1024 bytes. */
    fun hasPdfHeader(header: ByteArray): Boolean {
        val limit = minOf(header.size, 1024) - PDF_MAGIC.size
        for (start in 0..limit) {
            if (PDF_MAGIC.indices.all { header[start + it] == PDF_MAGIC[it] }) return true
        }
        return false
    }

    fun validateName(name: String): DocumentFieldError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> DocumentFieldError.NAME_EMPTY
            trimmed.length > VaultConfig.NAME_MAX_LENGTH -> DocumentFieldError.NAME_TOO_LONG
            else -> null
        }
    }

    fun validateNotes(notes: String): DocumentFieldError? =
        if (notes.trim().length > VaultConfig.NOTES_MAX_LENGTH) DocumentFieldError.NOTES_TOO_LONG else null

    /** "blood_report_march.pdf" -> "blood_report_march". Used to pre-fill the name field. */
    fun suggestedName(fileName: String?): String {
        val base = fileName.orEmpty().trim().removeSuffix(".pdf").removeSuffix(".PDF").trim()
        return base.take(VaultConfig.NAME_MAX_LENGTH)
    }
}
