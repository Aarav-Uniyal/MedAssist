package com.example.medassist.data.local

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Log
import com.example.medassist.data.repository.VaultError
import com.example.medassist.domain.validation.DocumentValidator
import com.example.medassist.domain.validation.PdfProblem
import com.example.medassist.domain.vault.VaultConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

/** A PDF the user picked, already checked and read into memory (at most 10 MB). */
class PickedPdf(val fileName: String?, val bytes: ByteArray)

sealed interface PickedPdfResult {
    data class Ok(val pdf: PickedPdf) : PickedPdfResult
    data class Invalid(val error: VaultError) : PickedPdfResult
}

/** Reads a file chosen in the system file picker. An interface so ViewModel tests can fake it. */
fun interface PickedPdfReader {
    /** [uri] is the picker's content:// URI as a string (a String keeps ViewModels free of Android types). */
    suspend fun read(uri: String): PickedPdfResult
}

private const val TAG = "MedAssistVault"

class AndroidPickedPdfReader(context: Context) : PickedPdfReader {

    private val appContext = context.applicationContext

    override suspend fun read(uri: String): PickedPdfResult = withContext(Dispatchers.IO) {
        val contentUri = Uri.parse(uri)
        val resolver = appContext.contentResolver
        try {
            var fileName: String? = null
            var reportedSize = -1L
            resolver.query(contentUri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(0)
                        if (!cursor.isNull(1)) reportedSize = cursor.getLong(1)
                    }
                }
            // Refuse oversized files before reading them, when the size is known.
            if (reportedSize > VaultConfig.MAX_FILE_BYTES) return@withContext PickedPdfResult.Invalid(VaultError.FILE_TOO_LARGE)

            val bytes = resolver.openInputStream(contentUri)?.use { input ->
                // Read at most one byte more than the limit, so a lying "size" cannot fill the memory.
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > VaultConfig.MAX_FILE_BYTES) return@withContext PickedPdfResult.Invalid(VaultError.FILE_TOO_LARGE)
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            } ?: return@withContext PickedPdfResult.Invalid(VaultError.UNREADABLE)

            when (DocumentValidator.checkPdf(resolver.getType(contentUri), bytes.size.toLong(), bytes.copyOf(minOf(bytes.size, 1024)))) {
                PdfProblem.EMPTY -> return@withContext PickedPdfResult.Invalid(VaultError.FILE_EMPTY)
                PdfProblem.TOO_LARGE -> return@withContext PickedPdfResult.Invalid(VaultError.FILE_TOO_LARGE)
                PdfProblem.NOT_PDF -> return@withContext PickedPdfResult.Invalid(VaultError.NOT_PDF)
                null -> Unit
            }
            checkOpens(bytes)?.let { return@withContext PickedPdfResult.Invalid(it) }
            PickedPdfResult.Ok(PickedPdf(fileName, bytes))
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to read the picked file", e)
            PickedPdfResult.Invalid(VaultError.UNREADABLE)
        } catch (e: IOException) {
            Log.w(TAG, "Reading the picked file failed", e)
            PickedPdfResult.Invalid(if (e.message.orEmpty().contains("ENOSPC")) VaultError.DEVICE_STORAGE_FULL else VaultError.UNREADABLE)
        }
    }

    /**
     * Opens the PDF with Android's own renderer, so a broken or password-protected file is caught
     * now, not after it has been uploaded. Uses a temporary private file that is always deleted.
     */
    private fun checkOpens(bytes: ByteArray): VaultError? {
        val temp = File.createTempFile("check", ".pdf", appContext.cacheDir)
        return try {
            temp.writeBytes(bytes)
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                PdfRenderer(fd).use { renderer -> if (renderer.pageCount <= 0) VaultError.CORRUPTED else null }
            }
        } catch (e: SecurityException) {
            VaultError.PASSWORD_PROTECTED
        } catch (e: IOException) {
            if (e.message.orEmpty().contains("ENOSPC")) VaultError.DEVICE_STORAGE_FULL else VaultError.CORRUPTED
        } catch (e: RuntimeException) {
            VaultError.CORRUPTED
        } finally {
            temp.delete()
        }
    }
}
