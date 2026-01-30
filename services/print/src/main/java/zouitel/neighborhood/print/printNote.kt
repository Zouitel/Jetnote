package zouitel.neighborhood.print

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
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
import android.text.StaticLayout
import android.text.TextPaint
import java.io.FileOutputStream
import androidx.core.graphics.withTranslation

fun printNote(
    context: Context,
    title: String,
    description: String,
    tasks: List<Pair<String?, Boolean>> = emptyList()
) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

    val printAdapter = object : PrintDocumentAdapter() {
        private var pdfDocument: PrintedPdfDocument? = null

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

            // Create a new PdfDocument with the requested page attributes
            pdfDocument = PrintedPdfDocument(context, newAttributes)

            // Just create one page
            callback.onLayoutFinished(
                PrintDocumentInfo.Builder(title.ifEmpty { "Untitled" })
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build(),
                true
            )
        }

        override fun onWrite(
            pages: Array<PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                val document = pdfDocument ?: return

                // Create page and draw content
                val page = document.startPage(0)
                val canvas = page.canvas

                // Calculate available width and height
                val contentWidth = canvas.width
//                val contentHeight = canvas.height

                // Draw the content
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 36f
                }

                // Title
                canvas.drawText(title, 72f, 72f, paint)

                // Description
                paint.textSize = 24f
                var yPosition = 144f

                // Create a StaticLayout to handle text wrapping
                val textPaint = TextPaint(paint)
                val availableWidth = contentWidth - 144 // 72 margin on each side

                val staticLayout = StaticLayout.Builder.obtain(
                    description,
                    0,
                    description.length,
                    textPaint,
                    availableWidth
                )
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .setIncludePad(false)
                    .build()
                canvas.withTranslation(72f, yPosition) {
                    staticLayout.draw(this)
                }

                // Update yPosition after drawing the description
                yPosition += staticLayout.height + 48f // Add some spacing

                // Tasks
                paint.textSize = 20f
                tasks.forEach { task ->
                    if (yPosition + paint.textSize > canvas.height - 72f) {
                        // TODO: Handle content exceeding page height (e.g., start a new page)
                        return@forEach
                    }

                    // Apply strikethrough for done tasks
                    paint.isStrikeThruText = task.second

                    val taskText = (if (task.second) "✅ " else "⬛ ") + task.first
                    canvas.drawText(taskText, 72f, yPosition, paint)

                    // Reset strikethrough for the next item
                    paint.isStrikeThruText = false
                    yPosition += paint.textSize + 12f // Add spacing between tasks
                }

                document.finishPage(page)

                // Write to output
                document.writeTo(FileOutputStream(destination.fileDescriptor))

                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdfDocument?.close()
                pdfDocument = null
            }
        }

        override fun onFinish() {
            pdfDocument?.close()
            pdfDocument = null
        }
    }

    printManager.print(title.ifEmpty { "Untitled" }, printAdapter, null)
}