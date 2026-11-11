package zouitel.neighborhood.workspace.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.workspace.model.Data
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Converts one Jetnote note to portable document formats.
 *
 * This class deliberately has no persistence dependency: callers provide the selected note
 * and write the returned bytes to the Android Storage Access Framework destination.
 */
object NoteExporter {

    enum class Format(val extension: String, val mimeType: String) {
        EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        MARKDOWN("md", "text/markdown"),
        AUDIO("wav", "audio/wav"),
        TEXT("txt", "text/plain"),
        PDF("pdf", "application/pdf"),
        WORD("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        SUBTITLES("srt", "application/x-subrip")
    }

    data class ExportDocument(
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray
    )

    fun plainText(note: Data): String = buildString {
        note.blocks.forEach { block ->
            val text = (block.content as? BlockContent.Text)?.text.orEmpty()
            when (val type = block.type) {
                is BlockType.Heading -> appendLine(text)
                is BlockType.BulletList -> appendLine("• $text")
                is BlockType.NumberedList -> appendLine("${type.number}. $text")
                is BlockType.Todo -> appendLine("${if (type.checked) "[x]" else "[ ]"} $text")
                BlockType.Quote -> appendLine("> $text")
                BlockType.Divider -> appendLine("---")
                else -> if (text.isNotBlank()) appendLine(text)
            }
        }
    }.trimEnd()

    fun markdown(note: Data): String = buildString {
        if (note.title.isNotBlank()) appendLine("# ${note.title.trim()}").appendLine()
        note.blocks.forEach { block ->
            val text = (block.content as? BlockContent.Text)?.text.orEmpty()
            val indent = "  ".repeat(block.attributes.indentationLevel)
            when (val type = block.type) {
                is BlockType.Heading -> appendLine("${"#".repeat(type.level)} $text")
                is BlockType.BulletList -> appendLine("$indent- $text")
                is BlockType.NumberedList -> appendLine("$indent${type.number}. $text")
                is BlockType.Todo -> appendLine("$indent- [${if (type.checked) "x" else " "}] $text")
                BlockType.Quote -> appendLine("> $text")
                BlockType.Divider -> appendLine("---")
                else -> if (text.isNotBlank()) appendLine(text)
            }
        }
    }.trimEnd()

    fun textDocument(note: Data): ExportDocument =
        document(note, Format.TEXT, plainText(note).toByteArray(Charsets.UTF_8))

    fun markdownDocument(note: Data): ExportDocument =
        document(note, Format.MARKDOWN, markdown(note).toByteArray(Charsets.UTF_8))

    fun pdf(note: Data): ExportDocument {
        val pdf = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val lines = buildList {
            if (note.title.isNotBlank()) add(note.title)
            addAll(plainText(note).lines().flatMap { line -> wrap(line, 88) })
        }
        val pageWidth = 595
        val pageHeight = 842
        val left = 48f
        val lineHeight = 18f
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 64f
        lines.forEachIndexed { index, line ->
            if (y > pageHeight - 56f) {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 64f
            }
            canvas.drawText(line, left, y, if (index == 0 && note.title.isNotBlank()) titlePaint else paint)
            y += if (index == 0 && note.title.isNotBlank()) 32f else lineHeight
        }
        pdf.finishPage(page)
        val output = ByteArrayOutputStream()
        pdf.writeTo(output)
        pdf.close()
        return document(note, Format.PDF, output.toByteArray())
    }

    /** Creates a standards-compliant minimal XLSX workbook with title and one row per block. */
    fun excel(note: Data): ExportDocument {
        val rows = buildList {
            add(listOf("Title", note.title))
            add(listOf("Type", "Content"))
            note.blocks.forEach { block ->
                val value = (block.content as? BlockContent.Text)?.text.orEmpty()
                add(listOf(block.type.displayName, value))
            }
        }
        val sheetRows = rows.mapIndexed { rowIndex, cells ->
            val xmlCells = cells.mapIndexed { colIndex, value ->
                val ref = "${columnName(colIndex + 1)}${rowIndex + 1}"
                """<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${xml(value)}</t></is></c>"""
            }.joinToString("")
            """<row r="${rowIndex + 1}">$xmlCells</row>"""
        }.joinToString("")
        val workbook = zipBytes(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Note" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            "xl/worksheets/sheet1.xml" to """<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>$sheetRows</sheetData></worksheet>"""
        )
        return document(note, Format.EXCEL, workbook)
    }

    /** Creates an editable Word document using the OpenXML package format. */
    fun word(note: Data): ExportDocument {
        val paragraphs = buildString {
            if (note.title.isNotBlank()) append("""<w:p><w:pPr><w:pStyle w:val="Title"/></w:pPr><w:r><w:t xml:space="preserve">${xml(note.title)}</w:t></w:r></w:p>""")
            note.blocks.forEach { block ->
                val text = (block.content as? BlockContent.Text)?.text.orEmpty()
                val value = when (val type = block.type) {
                    is BlockType.Heading -> """<w:pPr><w:pStyle w:val="Heading${type.level}"/></w:pPr>"""
                    is BlockType.BulletList -> """<w:pPr><w:ind w:left="360"/></w:pPr>"""
                    is BlockType.NumberedList -> """<w:pPr><w:ind w:left="360"/></w:pPr>"""
                    BlockType.Quote -> """<w:pPr><w:pStyle w:val="Quote"/></w:pPr>"""
                    BlockType.Divider -> """<w:p><w:r><w:t>────────────────────</w:t></w:r></w:p>"""
                    else -> ""
                }
                if (block.type == BlockType.Divider) append(value)
                else append("<w:p>$value<w:r><w:t xml:space=\"preserve\">${xml(text)}</w:t></w:r></w:p>")
            }
        }
        val bytes = zipBytes(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""",
            "word/document.xml" to """<?xml version="1.0" encoding="UTF-8"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$paragraphs<w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr></w:body></w:document>"""
        )
        return document(note, Format.WORD, bytes)
    }

    /**
     * Synthesizes the note text to WAV with Android TextToSpeech.
     * The caller should run this from a background coroutine.
     */
    suspend fun audio(context: Context, note: Data, locale: Locale = Locale.getDefault()): ExportDocument {
        val text = listOf(note.title, plainText(note)).filter(String::isNotBlank).joinToString(". ")
        require(text.isNotBlank()) { "There is no text to narrate." }
        val output = File(context.cacheDir, "note-export-${UUID.randomUUID()}.wav")
        val tts = suspendCancellableCoroutine<TextToSpeech> { continuation ->
            val engine = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) continuation.resume(enginePlaceholder.get() ?: return@TextToSpeech)
                else continuation.resumeWithException(IllegalStateException("Text-to-speech initialization failed ($status)."))
            }
            enginePlaceholder.set(engine)
            continuation.invokeOnCancellation { engine.shutdown() }
        }
        try {
            tts.language = locale
            val utteranceId = UUID.randomUUID().toString()
            suspendCancellableCoroutine<Unit> { continuation ->
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) = Unit
                    override fun onDone(id: String?) {
                        if (id == utteranceId && continuation.isActive) continuation.resume(Unit)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        if (id == utteranceId && continuation.isActive) continuation.resumeWithException(
                            IllegalStateException("Text-to-speech synthesis failed.")
                        )
                    }
                    override fun onError(id: String?, errorCode: Int) = onError(id)
                })
                val result = tts.synthesizeToFile(text, Bundle(), output, utteranceId)
                if (result == TextToSpeech.ERROR && continuation.isActive) {
                    continuation.resumeWithException(IllegalStateException("Text-to-speech could not start."))
                }
                continuation.invokeOnCancellation { output.delete() }
            }
            return document(note, Format.AUDIO, output.readBytes())
        } finally {
            tts.shutdown()
            output.delete()
        }
    }

    /** SRT captions are segmented from the same narration text; timing is estimated at 150 wpm. */
    fun subtitles(note: Data, wordsPerMinute: Int = 150): ExportDocument {
        require(wordsPerMinute in 80..240)
        val text = listOf(note.title, plainText(note)).filter(String::isNotBlank).joinToString(". ")
        val words = text.split(Regex("\\s+")).filter(String::isNotBlank)
        val chunks = words.chunked(10)
        var elapsedMs = 0L
        val srt = chunks.mapIndexed { index, chunk ->
            val duration = (chunk.size * 60_000L / wordsPerMinute).coerceAtLeast(1_000L)
            val start = elapsedMs
            val end = start + duration
            elapsedMs = end
            "${index + 1}\n${timestamp(start)} --> ${timestamp(end)}\n${chunk.joinToString(" ")}"
        }.joinToString("\n\n")
        return document(note, Format.SUBTITLES, srt.toByteArray(Charsets.UTF_8))
    }

    private fun document(note: Data, format: Format, bytes: ByteArray) =
        ExportDocument("${safeName(note.title)}.${format.extension}", format.mimeType, bytes)

    private fun safeName(value: String): String = value.trim()
        .ifBlank { "jetnote" }
        .replace(Regex("[^A-Za-z0-9._ -]"), "_")
        .replace(Regex("\\s+"), "_")
        .take(80)

    private fun wrap(value: String, maxChars: Int): List<String> {
        if (value.isEmpty()) return listOf("")
        val result = mutableListOf<String>()
        var line = StringBuilder()
        value.split(Regex("\\s+")).forEach { word ->
            if (line.isNotEmpty() && line.length + word.length + 1 > maxChars) {
                result += line.toString()
                line = StringBuilder()
            }
            if (line.isNotEmpty()) line.append(' ')
            line.append(word)
        }
        if (line.isNotEmpty()) result += line.toString()
        return result
    }

    private fun timestamp(ms: Long): String {
        val hours = ms / 3_600_000
        val minutes = (ms % 3_600_000) / 60_000
        val seconds = (ms % 60_000) / 1_000
        val millis = ms % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }

    private fun columnName(number: Int): String {
        var n = number
        val result = StringBuilder()
        while (n > 0) {
            val remainder = (n - 1) % 26
            result.insert(0, ('A'.code + remainder).toChar())
            n = (n - 1) / 26
        }
        return result.toString()
    }

    private fun xml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun zipBytes(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private object enginePlaceholder : ThreadLocal<TextToSpeech>()
}
