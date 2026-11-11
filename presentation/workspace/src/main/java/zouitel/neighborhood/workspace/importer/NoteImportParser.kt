package zouitel.neighborhood.workspace.importer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.model.Data
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Extracts text from portable note formats without granting broad storage access.
 * Input is read through the URI permission returned by Android's document picker.
 */
object NoteImportParser {
    private const val MAX_INPUT_BYTES = 25 * 1024 * 1024
    private const val MAX_TEXT_CHARS = 500_000

    data class ParsedNote(
        val fileName: String,
        val title: String,
        val blocks: List<Block>
    )

    fun parse(context: Context, uri: Uri): ParsedNote {
        val fileName = displayName(context, uri)
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val content = when (extension) {
            "txt" -> readText(context, uri)
            "md", "markdown" -> readText(context, uri)
            "srt" -> readText(context, uri)
            "docx" -> readDocx(context, uri)
            "xlsx" -> readXlsx(context, uri)
            "pdf" -> readPdf(context, uri)
            else -> throw IllegalArgumentException("Unsupported file type: .$extension")
        }.take(MAX_TEXT_CHARS)

        if (content.isBlank()) {
            throw IllegalArgumentException("No readable text was found in $fileName.")
        }

        val title = fileName.substringBeforeLast('.', fileName)
            .replace(Regex("[_]+"), " ")
            .trim()
            .ifBlank { "Imported note" }

        val blocks = if (extension == "md" || extension == "markdown") {
            markdownBlocks(content)
        } else if (extension == "srt") {
            subtitleBlocks(content)
        } else {
            paragraphBlocks(content)
        }

        return ParsedNote(fileName, title, blocks.ifEmpty { listOf(Block.paragraph(content)) })
    }

    private fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index)?.let { return it }
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "imported.txt"
    }

    private fun readText(context: Context, uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = readBounded(input)
            bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        } ?: error("Unable to open the selected file.")

    private fun readPdf(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context.applicationContext)
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val document = PDDocument.load(input)
            document.use { PDFTextStripper().getText(it) }
        } ?: error("Unable to open the selected PDF.")
    }

    private fun readDocx(context: Context, uri: Uri): String {
        val xml = readZipEntry(context, uri, "word/document.xml")
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xml.reader())
        }
        val paragraphs = mutableListOf<String>()
        var current = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (parser.name == "p") current = StringBuilder()
                XmlPullParser.TEXT -> if (parser.depth > 0 && parser.text.isNotBlank()) {
                    // Only text nodes inside Word runs are relevant; property nodes contain no text.
                    if (parser.name == null || parser.name == "t") current.append(parser.text)
                }
                XmlPullParser.END_TAG -> if (parser.name == "p") {
                    current.toString().trim().takeIf(String::isNotBlank)?.let(paragraphs::add)
                }
            }
            event = parser.next()
        }
        return paragraphs.joinToString("\n\n")
    }

    private fun readXlsx(context: Context, uri: Uri): String {
        val sharedStringsXml = runCatching { readZipEntry(context, uri, "xl/sharedStrings.xml") }.getOrNull()
        val sharedStrings = sharedStringsXml?.let(::readSharedStrings).orEmpty()
        val sheetPath = listOf("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml")
            .firstOrNull { path -> runCatching { readZipEntry(context, uri, path) }.isSuccess }
            ?: error("The workbook does not contain a readable worksheet.")
        val sheetXml = readZipEntry(context, uri, sheetPath)
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(sheetXml.reader())
        }
        val rows = mutableListOf<String>()
        var row = mutableListOf<String>()
        var cellType = ""
        var cellValue = ""
        var inValue = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "row" -> row = mutableListOf()
                    "c" -> {
                        cellType = parser.getAttributeValue(null, "t").orEmpty()
                        cellValue = ""
                    }
                    "v", "t" -> inValue = true
                }
                XmlPullParser.TEXT -> if (inValue) cellValue += parser.text
                XmlPullParser.END_TAG -> when (parser.name) {
                    "v", "t" -> inValue = false
                    "c" -> {
                        val value = if (cellType == "s") {
                            sharedStrings.getOrNull(cellValue.toIntOrNull() ?: -1).orEmpty()
                        } else cellValue
                        row += value
                    }
                    "row" -> if (row.any(String::isNotBlank)) rows += row.joinToString(" | ")
                }
            }
            event = parser.next()
        }
        return rows.joinToString("\n")
    }

    private fun readSharedStrings(xml: String): List<String> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(xml.reader())
        }
        val values = mutableListOf<String>()
        var current = StringBuilder()
        var inItem = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "si" -> { current = StringBuilder(); inItem = true }
                }
                XmlPullParser.TEXT -> if (inItem && parser.depth > 0) current.append(parser.text)
                XmlPullParser.END_TAG -> if (parser.name == "si") {
                    values += current.toString()
                    inItem = false
                }
            }
            event = parser.next()
        }
        return values
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= MAX_INPUT_BYTES) { "File is larger than 25 MB." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun readZipEntry(context: Context, uri: Uri, entryName: String): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == entryName) {
                        val bytes = readBounded(zip)
                        return bytes.toString(Charsets.UTF_8)
                    }
                }
            }
        } ?: error("Unable to open the selected document.")
        error("The file is missing $entryName.")
    }

    private fun markdownBlocks(content: String): List<Block> = content
        .replace("\r\n", "\n")
        .lines()
        .mapNotNull { raw ->
            val line = raw.trimEnd()
            when {
                line.isBlank() -> null
                line.matches(Regex("^#{1,6}\\s+.*")) -> {
                    val level = line.takeWhile { it == '#' }.length
                    Block.heading(level, line.drop(level).trim())
                }
                line == "---" || line == "***" -> Block.divider()
                line.startsWith("> ") -> Block.paragraph(line.removePrefix("> ")).let {
                    it.copy(type = zouitel.neighborhood.markdown.core.BlockType.Quote)
                }
                line.matches(Regex("^[-*+]\\s+\\[[ xX]]\\s+.*")) -> {
                    val checked = line.contains(Regex("\\[[xX]]"))
                    Block.todo(line.substringAfter("] ").trim(), checked)
                }
                line.matches(Regex("^[-*+]\\s+.*")) -> Block.bulletList(line.drop(2).trim())
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val number = line.substringBefore('.').toIntOrNull() ?: 1
                    Block.numberedList(line.substringAfter('.').trim(), number)
                }
                else -> Block.paragraph(line)
            }
        }

    private fun subtitleBlocks(content: String): List<Block> = content
        .replace("\r\n", "\n")
        .split(Regex("\\n\\s*\\n"))
        .mapNotNull { cue ->
            val lines = cue.lines().filter { line ->
                val value = line.trim()
                value.isNotBlank() &&
                    !value.matches(Regex("\\d+")) &&
                    !value.contains("-->")
            }
            lines.joinToString(" ").trim().takeIf(String::isNotBlank)?.let(Block::paragraph)
        }

    private fun paragraphBlocks(content: String): List<Block> = content
        .replace("\r\n", "\n")
        .split(Regex("\\n\\s*\\n"))
        .mapNotNull { it.trim().takeIf(String::isNotBlank)?.let(Block::paragraph) }
}
