package zouitel.neighborhood.document

import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Page
import com.artifex.mupdf.fitz.PDFDocument
import com.artifex.mupdf.fitz.StructuredText
import java.io.File
import java.io.IOException

object PdfParser {
    private const val MAX_FILE_BYTES = 250L * 1024 * 1024
    private const val MAX_PAGES = 10_000
    private const val MAX_EXTRACTED_CHARS = 10 * 1024 * 1024

    /**
     * Extracts text from a PDF. MuPDF objects are explicitly destroyed because
     * they own native resources that should not be left to finalization.
     */
    fun parserPdf(file: File): String {
        require(file.isFile && file.canRead()) { "PDF file is not readable" }
        require(file.length() <= MAX_FILE_BYTES) { "PDF exceeds the size limit" }

        var document: Document? = null
        try {
            document = PDFDocument.openDocument(file.absolutePath)
            if (!document.isPDF) throw IOException("File is not a supported PDF document")
            val pdf = document.asPDF()
            if (pdf.needsPassword()) throw IOException("PDF is password-protected")
            val pageCount = pdf.countPages()
            if (pageCount > MAX_PAGES) throw IOException("PDF contains too many pages")

            val result = StringBuilder()
            for (index in 0 until pageCount) {
                var page: Page? = null
                var structuredText: StructuredText? = null
                try {
                    page = pdf.loadPage(index)
                    structuredText = page.toStructuredText()
                    result.append("---\nPage ${index + 1}:\n")
                    result.append(structuredText.asText())
                    result.append('\n')
                    if (result.length > MAX_EXTRACTED_CHARS) {
                        throw IOException("Extracted PDF text exceeds the size limit")
                    }
                } finally {
                    structuredText?.destroy()
                    page?.destroy()
                }
            }
            return result.toString()
        } finally {
            document?.destroy()
        }
    }
}
