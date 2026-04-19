package zouitel.neighborhood.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.File

// ── Public entry point ────────────────────────────────────────────────────────

/**
 * Submits a print job for any document type supported by MuPDF
 * (PDF, DOCX, XLSX, PPTX, TXT, images …).
 *
 * Each page is rendered at [renderDpi] DPI and written to the system print
 * spooler via [PrintDocumentAdapter].  The user sees the standard Android
 * print preview / printer-selection UI.
 *
 * @param context      Activity or Application context.
 * @param documentPath Absolute file-system path to the document.
 * @param jobName      Label shown in the print queue (defaults to the file name).
 * @param renderDpi    Render resolution — 300 DPI gives print-quality output.
 */
fun printDocumentFromUri(
    context     : Context,
    documentPath: String,
    jobName     : String = File(documentPath).name,
    renderDpi   : Int    = 300
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
        Toast.makeText(context, "Print service unavailable.", Toast.LENGTH_SHORT).show()
        return
    }

    // Open the document once here to validate it before handing off to the adapter.
    val document = runCatching { Document.openDocument(documentPath) }.getOrElse {
        Toast.makeText(context, "Cannot open document: ${it.message}", Toast.LENGTH_SHORT).show()
        return
    }

    val pageCount = runCatching { document.countPages() }.getOrElse { 0 }
    if (pageCount == 0) {
        document.destroy()
        Toast.makeText(context, "Document has no printable pages.", Toast.LENGTH_SHORT).show()
        return
    }

    val adapter = MuPdfPrintDocumentAdapter(
        document  = document,
        pageCount = pageCount,
        jobName   = jobName,
        renderDpi = renderDpi
    )

    printManager.print(jobName, adapter, PrintAttributes.Builder().build())
}

// ── Adapter ───────────────────────────────────────────────────────────────────

/**
 * [PrintDocumentAdapter] that renders document pages via MuPDF.
 *
 * Lifecycle:
 *  1. [onLayout]  — called when the user changes print settings; returns page count.
 *  2. [onWrite]   — called when the spooler needs page content; renders each
 *                   requested page range into a [PdfDocument] and writes it to
 *                   the provided [ParcelFileDescriptor].
 *  3. [onFinish]  — destroys the MuPDF document and cancels the coroutine scope.
 */
private class MuPdfPrintDocumentAdapter(
    private val document : Document,
    private val pageCount: Int,
    private val jobName  : String,
    private val renderDpi: Int
) : PrintDocumentAdapter() {

    // Each adapter instance owns its own scope so IO work cancels cleanly on finish.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── onLayout ─────────────────────────────────────────────────────────────

    override fun onLayout(
        oldAttributes : PrintAttributes?,
        newAttributes : PrintAttributes,
        cancellation  : CancellationSignal?,
        callback      : LayoutResultCallback,
        extras        : Bundle?
    ) {
        if (cancellation?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()

        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    // ── onWrite ──────────────────────────────────────────────────────────────

    override fun onWrite(
        pages      : Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellation: CancellationSignal?,
        callback   : WriteResultCallback
    ) {
        scope.launch {
            val writtenRanges = mutableListOf<PageRange>()
            val pdfDoc        = PdfDocument()

            try {
                val pagesToRender = expandRanges(pages, pageCount)

                withContext(Dispatchers.IO) {
                    for (pageIndex in pagesToRender) {
                        if (cancellation?.isCanceled == true) break

                        val bitmap = renderPage(pageIndex) ?: continue

                        // Create a PDF page sized to match the bitmap at the chosen DPI.
                        val widthPts  = (bitmap.width  * 72f / renderDpi).toInt()
                        val heightPts = (bitmap.height * 72f / renderDpi).toInt()

                        val pageInfo = PdfDocument.PageInfo.Builder(widthPts, heightPts, pageIndex + 1).create()
                        val pdfPage  = pdfDoc.startPage(pageInfo)

                        drawBitmapOnCanvas(pdfPage.canvas, bitmap, widthPts, heightPts)

                        pdfDoc.finishPage(pdfPage)
                        bitmap.recycle()
                        writtenRanges.add(PageRange(pageIndex, pageIndex))
                    }

                    // Write the final PDF to the spooler's file descriptor.
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        pdfDoc.writeTo(out)
                    }
                }

                if (cancellation?.isCanceled == true) {
                    callback.onWriteCancelled()
                } else {
                    callback.onWriteFinished(writtenRanges.toTypedArray())
                }

            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdfDoc.close()
            }
        }
    }

    // ── onFinish ─────────────────────────────────────────────────────────────

    override fun onFinish() {
        runCatching { document.destroy() }
        scope.cancel()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Renders a single document page to a [Bitmap] using MuPDF.
     * Scale factor is derived from [renderDpi] (MuPDF renders at 72 DPI by default,
     * so scale = renderDpi / 72).
     */
    private fun renderPage(index: Int): Bitmap? = runCatching {
        val scale  = renderDpi / 72f
        val page   = document.loadPage(index)
        val matrix = com.artifex.mupdf.fitz.Matrix(scale, scale)
        AndroidDrawDevice.drawPage(page, matrix)
    }.getOrNull()

    /**
     * Draws [bitmap] onto [canvas] scaled to fit [destWidth] × [destHeight] points,
     * centred on a white background.
     */
    private fun drawBitmapOnCanvas(
        canvas   : Canvas,
        bitmap   : Bitmap,
        destWidth : Int,
        destHeight: Int
    ) {
        canvas.drawColor(Color.WHITE)

        val scaleX = destWidth.toFloat()  / bitmap.width
        val scaleY = destHeight.toFloat() / bitmap.height
        val scale  = minOf(scaleX, scaleY)

        val left = (destWidth  - bitmap.width  * scale) / 2f
        val top  = (destHeight - bitmap.height * scale) / 2f

        val matrix = Matrix().apply { setScale(scale, scale); postTranslate(left, top) }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    /**
     * Expands an array of [PageRange] objects (which may include
     * [PageRange.ALL_PAGES]) into a sorted, deduplicated list of zero-based page
     * indices bounded by [totalPages].
     */
    private fun expandRanges(ranges: Array<out PageRange>, totalPages: Int): List<Int> {
        val result = sortedSetOf<Int>()
        for (range in ranges) {
            val start = if (range == PageRange.ALL_PAGES) 0          else range.start
            val end   = if (range == PageRange.ALL_PAGES) totalPages - 1 else range.end
            for (i in start..end.coerceAtMost(totalPages - 1)) result.add(i)
        }
        return result.toList()
    }
}