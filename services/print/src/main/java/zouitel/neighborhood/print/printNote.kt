package zouitel.neighborhood.print

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import kotlinx.coroutines.CancellationException

private const val NOTE_MARGIN = 48f

fun printNote(
    context: Context,
    title: String,
    description: String,
    tasks: List<Pair<String?, Boolean>> = emptyList()
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        ?: return
    val jobTitle = title.ifBlank { "Untitled" }

    val printAdapter = object : PrintDocumentAdapter() {
        private var pdfDocument: PrintedPdfDocument? = null
        private var contentLayout: StaticLayout? = null
        private var contentHeight = 0
        private var pageCount = 0

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            metadata: Bundle?
        ) {
            if (cancellationSignal.isCanceled) {
                callback.onLayoutCancelled()
                return
            }

            runCatching {
                pdfDocument?.close()
                val document = PrintedPdfDocument(context, newAttributes)
                try {
                    val measurePage = document.startPage(0)
                    val canvas = measurePage.canvas
                    val availableWidth = (canvas.width - NOTE_MARGIN * 2).toInt().coerceAtLeast(1)
                    contentHeight = (canvas.height - NOTE_MARGIN * 2).toInt().coerceAtLeast(1)
                    contentLayout = createContentLayout(availableWidth)
                    document.finishPage(measurePage)
                    pageCount = (((contentLayout?.height ?: 0) + contentHeight - 1) / contentHeight)
                        .coerceAtLeast(1)
                    pdfDocument = document
                    pageCount
                } catch (error: Exception) {
                    document.close()
                    throw error
                }
            }.onSuccess { count ->
                val info = PrintDocumentInfo.Builder(jobTitle)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(count)
                    .build()
                callback.onLayoutFinished(info, oldAttributes != newAttributes)
            }.onFailure { error ->
                callback.onLayoutFailed(error.message ?: "Unable to prepare the note for printing.")
            }
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            val document = pdfDocument
            val layout = contentLayout
            if (document == null || layout == null) {
                destination.close()
                callback.onWriteFailed("The note has not been laid out for printing.")
                return
            }

            val selectedPages = expandPageRanges(pages, pageCount)
            if (selectedPages.isEmpty()) {
                destination.close()
                callback.onWriteFailed("No printable pages were selected.")
                return
            }

            try {
                selectedPages.forEach { pageIndex ->
                    if (cancellationSignal.isCanceled) throw CancellationException()
                    val page = document.startPage(pageIndex)
                    try {
                        page.canvas.drawColor(Color.WHITE)
                        page.canvas.save()
                        page.canvas.translate(NOTE_MARGIN, NOTE_MARGIN - pageIndex * contentHeight)
                        layout.draw(page.canvas)
                        page.canvas.restore()
                    } finally {
                        document.finishPage(page)
                    }
                }

                if (cancellationSignal.isCanceled) throw CancellationException()
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use(document::writeTo)
                callback.onWriteFinished(toPageRanges(selectedPages))
            } catch (_: CancellationException) {
                runCatching { destination.close() }
                callback.onWriteCancelled()
            } catch (error: Exception) {
                runCatching { destination.close() }
                callback.onWriteFailed(error.message ?: "Unable to write the note.")
            } finally {
                pdfDocument?.close()
                pdfDocument = null
                contentLayout = null
            }
        }

        override fun onFinish() {
            pdfDocument?.close()
            pdfDocument = null
            contentLayout = null
        }

        private fun createContentLayout(width: Int): StaticLayout {
            val content = SpannableStringBuilder()
            val titleStart = content.length
            content.append(title.ifBlank { "Untitled" }).append("\n\n")
            content.setSpan(RelativeSizeSpan(1.35f), titleStart, content.length, 0)

            if (description.isNotBlank()) content.append(description.trim()).append("\n\n")

            tasks.forEach { (task, completed) ->
                content.append(if (completed) "☑ " else "□ ")
                val taskStart = content.length
                content.append(task.orEmpty().ifBlank { "Untitled task" }).append("\n")
                if (completed && content.length > taskStart) {
                    content.setSpan(StrikethroughSpan(), taskStart, content.length - 1, 0)
                }
            }

            val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 12f * context.resources.displayMetrics.density
            }
            return StaticLayout.Builder.obtain(content, 0, content.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1f)
                .setIncludePad(true)
                .build()
        }

        private fun expandPageRanges(ranges: Array<out PageRange>, totalPages: Int): List<Int> {
            val result = sortedSetOf<Int>()
            ranges.forEach { range ->
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
            pageIndices.drop(1).forEach { index ->
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

    printManager.print(jobTitle, printAdapter, null)
}
