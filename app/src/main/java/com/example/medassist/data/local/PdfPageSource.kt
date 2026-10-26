package com.example.medassist.data.local

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

/** Draws the pages of one open PDF. An interface so the viewer ViewModel can be tested without a real PDF. */
interface PdfPageSource : Closeable {
    val pageCount: Int

    /** Renders page [index] (0-based) [widthPx] pixels wide, keeping the page's shape. */
    suspend fun render(index: Int, widthPx: Int): ImageBitmap
}

/** Opens [file] for the viewer. Throws IOException or SecurityException if it cannot be read. */
fun interface PdfPageSourceFactory {
    fun open(file: File): PdfPageSource
}

/** [PdfPageSource] using Android's built-in PdfRenderer, so no extra PDF library is needed. */
class AndroidPdfPageSource(file: File) : PdfPageSource {

    private val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = try {
        PdfRenderer(descriptor)
    } catch (e: Exception) {
        descriptor.close()
        throw e
    }

    // PdfRenderer can open only one page at a time, so all access goes through this lock.
    private val lock = Mutex()
    private var closed = false

    override val pageCount: Int = renderer.pageCount

    override suspend fun render(index: Int, widthPx: Int): ImageBitmap = lock.withLock {
        withContext(Dispatchers.IO) {
            check(!closed) { "Renderer is closed" }
            renderer.openPage(index).use { page ->
                val width = widthPx.coerceIn(1, MAX_WIDTH_PX)
                val height = (width.toLong() * page.height / page.width.coerceAtLeast(1)).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE) // PDFs assume white paper; the bitmap starts transparent
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap.asImageBitmap()
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        renderer.close()
        descriptor.close()
    }

    private companion object {
        /** Keeps one page bitmap under about 25 MB even for very tall pages. */
        const val MAX_WIDTH_PX = 2000
    }
}
