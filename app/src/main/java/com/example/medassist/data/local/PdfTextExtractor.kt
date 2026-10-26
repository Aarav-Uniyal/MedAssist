package com.example.medassist.data.local

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.example.medassist.domain.vault.ReportTextPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Result of extracting the selectable text layer from a PDF. Scanned-image OCR is intentionally not included. */
sealed interface PdfTextExtractionResult {
    data class Success(
        val text: String,
        val totalPages: Int,
        val pagesRead: Int,
        val truncated: Boolean,
    ) : PdfTextExtractionResult

    data class NoReadableText(val pageCount: Int) : PdfTextExtractionResult
    data object PasswordProtected : PdfTextExtractionResult
    data object Unreadable : PdfTextExtractionResult
}

/** Extracts text locally, before the app sends any report text to the AI service. */
fun interface PdfTextExtractor {
    suspend fun extract(file: File): PdfTextExtractionResult
}

/** PDFBox-Android implementation. The original PDF remains in the existing private vault cache. */
class AndroidPdfTextExtractor(context: Context) : PdfTextExtractor {
    private val appContext = context.applicationContext

    init {
        PDFBoxResourceLoader.init(appContext)
    }

    override suspend fun extract(file: File): PdfTextExtractionResult = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() <= 0L) return@withContext PdfTextExtractionResult.Unreadable

        try {
            PDDocument.load(file).use { document ->
                val totalPages = document.numberOfPages
                if (totalPages <= 0) return@withContext PdfTextExtractionResult.NoReadableText(0)

                // Bound CPU time and request size for very long or malformed documents.
                val pagesRead = minOf(totalPages, MAX_PAGES_PER_ANALYSIS)
                val stripper = PDFTextStripper().apply {
                    startPage = 1
                    endPage = pagesRead
                    sortByPosition = true
                }
                val prepared = ReportTextPolicy.prepare(stripper.getText(document))
                    ?: return@withContext PdfTextExtractionResult.NoReadableText(totalPages)

                PdfTextExtractionResult.Success(
                    text = prepared.text,
                    totalPages = totalPages,
                    pagesRead = pagesRead,
                    truncated = prepared.truncated || pagesRead < totalPages,
                )
            }
        } catch (_: InvalidPasswordException) {
            PdfTextExtractionResult.PasswordProtected
        } catch (_: IOException) {
            PdfTextExtractionResult.Unreadable
        } catch (_: SecurityException) {
            PdfTextExtractionResult.Unreadable
        } catch (_: RuntimeException) {
            PdfTextExtractionResult.Unreadable
        }
    }

    private companion object {
        const val MAX_PAGES_PER_ANALYSIS = 60
    }
}
