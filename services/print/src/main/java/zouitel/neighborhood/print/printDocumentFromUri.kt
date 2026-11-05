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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val MIN_RENDER_DPI = 72
private const val MAX_RENDER_DPI = 600
private const val MAX_BITMAP_PIXELS = 40_000_000L
private const val MAX_BITMAP_DIMENSION = 12_000

/**
 * Prints a local document supported by MuPDF.
 *
 * The document is owned by the adapter and is released after any active render
 * has stopped. [renderDpi] is constrained to avoid invalid or unbounded bitmap
 * allocations.
 */
fun printDocumentFromUri(
    context: Context,
    documentPath: String,
    jobName: String = File(documentPath).name,
    renderDpi: Int = 300
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
        Toast.makeText(context, "Print service unavailable.", Toast.LENGTH_SHORT).show()
        return
    }
    if (renderDpi !in MIN_RENDER_DPI..MAX_RENDER_DPI) {
        Toast.makeText(
            context,
            "Print resolution must be between $MIN_RENDER_DPI and $MAX_RENDER_DPI DPI.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val document = runCatching { Document.openDocument(documentPath) }.getOrElse {
        Toast.makeText(context, "Cannot open document: ${it.message}", Toast.LENGTH_SHORT).show()
        return
    }
    val pageCount = runCatching { document.countPages() }.getOrElse { 0 }
    if (pageCount <= 0) {
        document.destroy()
        Toast.makeText(context, "Document has no printable pages.", Toast.LENGTH_SHORT).show()
        return
    }

    printManager.print(
        jobName,
        MuPdfPrintDocumentAdapter(document, pageCount, jobName, renderDpi),
        PrintAttributes.Builder().build()
    )
}

private class MuPdfPrintDocumentAdapter(
    private val document: Document,
    private val pageCount: Int,
    private val jobName: String,
    private val renderDpi: Int
) : PrintDocumentAdapter() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val documentLock = Any()

    @Volatile private var finished = false
    @Volatile private var activeWrite: kotlinx.coroutines.Job? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellation: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (finished || cancellation?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(jobName)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(pageCount)
            .build()
        callback.onLayoutFinished(info, oldAttributes != newAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellation: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        if (finished) {
            destination.close()
            callback.onWriteFailed("The print job has already finished.")
            return
        }

        activeWrite = scope.launch {
            val pdfDocument = PdfDocument()
            val writtenPages = mutableListOf<Int>()
            try {
                val pagesToRender = expandRanges(pages, pageCount)
                if (pagesToRender.isEmpty()) {
                    throw IOException("No printable pages were selected.")
                }

                withContext(Dispatchers.IO) {
                    for (pageIndex in pagesToRender) {
                        if (finished || cancellation?.isCanceled == true) throw CancellationException()
                        val bitmap = renderPage(pageIndex)
                        try {
                            if (finished || cancellation?.isCanceled == true) throw CancellationException()
                            val widthPoints = (bitmap.width * 72f / renderDpi).toInt().coerceAtLeast(1)
                            val heightPoints = (bitmap.height * 72f / renderDpi).toInt().coerceAtLeast(1)
                            val pageInfo = PdfDocument.PageInfo.Builder(
                                widthPoints,
                                heightPoints,
                                pageIndex + 1
                            ).create()
                            val page = pdfDocument.startPage(pageInfo)
                            drawBitmapOnCanvas(page.canvas, bitmap, widthPoints, heightPoints)
                            pdfDocument.finishPage(page)
                            writtenPages += pageIndex
                        } finally {
                            bitmap.recycle()
                        }
                    }

                    if (finished || cancellation?.isCanceled == true) throw CancellationException()
                    ParcelFileDescriptor.AutoCloseOutputStream(destination).use(pdfDocument::writeTo)
                }

                if (finished || cancellation?.isCanceled == true) callback.onWriteCancelled()
                else callback.onWriteFinished(toPageRanges(writtenPages))
            } catch (cancelled: CancellationException) {
                runCatching { destination.close() }
                callback.onWriteCancelled()
            } catch (error: Exception) {
                runCatching { destination.close() }
                callback.onWriteFailed(error.message ?: "Unable to render the selected pages.")
            } finally {
                pdfDocument.close()
            }
        }
    }

    override fun onFinish() {
        if (finished) return
        finished = true
        scope.launch {
            activeWrite?.join()
            withContext(Dispatchers.IO) {
                synchronized(documentLock) { runCatching { document.destroy() } }
            }
            scope.cancel()
        }
    }

    private fun renderPage(index: Int): Bitmap = synchronized(documentLock) {
        if (finished) throw CancellationException()
        val page = document.loadPage(index)
        try {
            val bounds = page.getBounds()
            val scale = renderDpi / 72f
            val width = ((bounds.x1 - bounds.x0) * scale).toInt()
            val height = ((bounds.y1 - bounds.y0) * scale).toInt()
            val pixels = width.toLong() * height.toLong()
            if (width <= 0 || height <= 0 ||
                width > MAX_BITMAP_DIMENSION || height > MAX_BITMAP_DIMENSION ||
                pixels > MAX_BITMAP_PIXELS
            ) {
                throw IOException("Page ${index + 1} is too large to render at $renderDpi DPI.")
            }
            AndroidDrawDevice.drawPage(
                page,
                com.artifex.mupdf.fitz.Matrix(scale, scale)
            )
        } finally {
            page.destroy()
        }
    }

    private fun drawBitmapOnCanvas(
        canvas: Canvas,
        bitmap: Bitmap,
        destWidth: Int,
        destHeight: Int
    ) {
        canvas.drawColor(Color.WHITE)
        val scale = minOf(
            destWidth.toFloat() / bitmap.width,
            destHeight.toFloat() / bitmap.height
        )
        val left = (destWidth - bitmap.width * scale) / 2f
        val top = (destHeight - bitmap.height * scale) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(left, top)
        }
        canvas.drawBitmap(bitmap, matrix, null)
    }

    private fun expandRanges(ranges: Array<out PageRange>, totalPages: Int): List<Int> {
        val result = sortedSetOf<Int>()
        for (range in ranges) {
            val start = if (range == PageRange.ALL_PAGES) 0 else range.start
            val end = if (range == PageRange.ALL_PAGES) totalPages - 1 else range.end
            if (start <= end && end >= 0 && start < totalPages) {
                for (index in start.coerceAtLeast(0)..end.coerceAtMost(totalPages - 1)) {
                    result += index
                }
            }
        }
        return result.toList()
    }

    private fun toPageRanges(pageIndices: List<Int>): Array<PageRange> {
        if (pageIndices.isEmpty()) return emptyArray()
        val ranges = mutableListOf<PageRange>()
        var start = pageIndices.first()
        var end = start
        for (index in pageIndices.drop(1)) {
            if (index == end + 1) end = index
            else {
                ranges += PageRange(start, end)
                start = index
                end = index
            }
        }
        ranges += PageRange(start, end)
        return ranges.toTypedArray()
    }
}
