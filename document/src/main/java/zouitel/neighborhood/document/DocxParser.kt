package zouitel.neighborhood.document

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Extracts the main document body from a DOCX package.
 *
 * Limits are deliberately conservative because DOCX files are untrusted ZIP
 * archives and may contain highly-compressed XML.
 */
object DocxParser {
    private const val MAX_ARCHIVE_BYTES = 50L * 1024 * 1024
    private const val MAX_DOCUMENT_XML_BYTES = 20 * 1024 * 1024
    private const val MAX_ENTRIES = 1_000
    private const val MAX_XML_DEPTH = 128
    private const val MAX_OUTPUT_CHARS = 5 * 1024 * 1024

    fun parse(file: File): String {
        return try {
            require(file.isFile && file.canRead()) { "DOCX file is not readable" }
            require(file.length() <= MAX_ARCHIVE_BYTES) { "DOCX archive exceeds the size limit" }

            val documentXml = readDocumentXml(file)
                ?: return "Unable to find document content in DOCX file"
            parseDocumentXml(documentXml)
        } catch (e: Exception) {
            "Error parsing DOCX file: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    private fun readDocumentXml(file: File): ByteArray? {
        ZipInputStream(file.inputStream().buffered()).use { zip ->
            var entries = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                if (entries > MAX_ENTRIES) throw IOException("DOCX contains too many ZIP entries")

                if (entry.name == "word/document.xml") {
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_DOCUMENT_XML_BYTES) {
                            throw IOException("DOCX document XML exceeds the size limit")
                        }
                        output.write(buffer, 0, count)
                    }
                    return output.toByteArray()
                }
                zip.closeEntry()
            }
        }
        return null
    }

    private fun parseDocumentXml(xml: ByteArray): String {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(xml), "UTF-8")

        val output = StringBuilder()
        val paragraph = StringBuilder()
        val tableRows = mutableListOf<List<String>>()
        val tableCells = mutableListOf<String>()
        val cellText = StringBuilder()
        val numberingCounters = mutableMapOf<String, Int>()

        var inBody = false
        var paragraphDepth = -1
        var runDepth = -1
        var tableDepth = -1
        var rowDepth = -1
        var cellDepth = -1
        var bold = false
        var italic = false
        var headingLevel = 0
        var listId: String? = null
        var listLevel = 0
        var outputChars = 0

        fun appendText(value: String) {
            if (value.isEmpty()) return
            val formatted = when {
                bold && italic -> "***${escapeInline(value)}***"
                bold -> "**${escapeInline(value)}**"
                italic -> "*${escapeInline(value)}*"
                else -> escapeInline(value)
            }
            paragraph.append(formatted)
            if (paragraph.length + output.length > MAX_OUTPUT_CHARS) {
                throw IOException("Extracted document text exceeds the size limit")
            }
        }

        fun finishParagraph() {
            val text = paragraph.toString().trim()
            paragraph.setLength(0)
            if (text.isEmpty()) return

            val rendered = when {
                listId != null -> {
                    val key = "$listId:$listLevel"
                    val number = (numberingCounters[key] ?: 0) + 1
                    numberingCounters[key] = number
                    val indent = "  ".repeat(listLevel.coerceIn(0, 8))
                    "$indent$number. $text"
                }
                headingLevel > 0 -> "${"#".repeat(headingLevel.coerceIn(1, 6))} $text"
                else -> text
            }

            if (tableDepth >= 0 && cellDepth >= 0) {
                if (cellText.isNotEmpty()) cellText.append(' ')
                cellText.append(rendered)
            } else {
                output.append(rendered).append("\n\n")
                outputChars = output.length
            }
            headingLevel = 0
            listId = null
            listLevel = 0
        }

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.depth > MAX_XML_DEPTH) throw IOException("DOCX XML nesting is too deep")

            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "body" -> inBody = true
                        "p" -> if (inBody) {
                            paragraphDepth = parser.depth
                            paragraph.setLength(0)
                            headingLevel = 0
                            listId = null
                            listLevel = 0
                        }
                        "pStyle" -> if (paragraphDepth >= 0) {
                            val style = attribute(parser, "val").orEmpty()
                            val heading = Regex("(?i)^heading([1-6])$").matchEntire(style)
                            if (heading != null) headingLevel = heading.groupValues[1].toInt()
                        }
                        "numPr" -> if (paragraphDepth >= 0) {
                            // numId and ilvl are read from their child elements below.
                        }
                        "ilvl" -> if (paragraphDepth >= 0) {
                            listLevel = attribute(parser, "val")?.toIntOrNull()?.coerceIn(0, 8) ?: 0
                        }
                        "numId" -> if (paragraphDepth >= 0) {
                            listId = attribute(parser, "val")?.takeIf { it != "0" }
                        }
                        "tbl" -> if (inBody) {
                            tableDepth = parser.depth
                            tableRows.clear()
                        }
                        "tr" -> if (tableDepth >= 0) {
                            rowDepth = parser.depth
                            tableCells.clear()
                        }
                        "tc" -> if (rowDepth >= 0) {
                            cellDepth = parser.depth
                            cellText.setLength(0)
                        }
                        "r" -> if (paragraphDepth >= 0) runDepth = parser.depth
                        "b" -> if (runDepth >= 0) bold = attribute(parser, "val") != "0" && attribute(parser, "val") != "false"
                        "i" -> if (runDepth >= 0) italic = attribute(parser, "val") != "0" && attribute(parser, "val") != "false"
                        "t" -> if (paragraphDepth >= 0) {
                            val value = parser.nextText()
                            appendText(value)
                        }
                        "tab" -> if (paragraphDepth >= 0) appendText("\t")
                        "br", "cr" -> if (paragraphDepth >= 0) appendText("\n")
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "r" -> if (parser.depth == runDepth) {
                            runDepth = -1
                            bold = false
                            italic = false
                        }
                        "p" -> if (parser.depth == paragraphDepth) {
                            finishParagraph()
                            paragraphDepth = -1
                        }
                        "tc" -> if (parser.depth == cellDepth) {
                            tableCells.add(cellText.toString().trim())
                            cellText.setLength(0)
                            cellDepth = -1
                        }
                        "tr" -> if (parser.depth == rowDepth) {
                            if (tableCells.isNotEmpty()) tableRows.add(tableCells.toList())
                            rowDepth = -1
                        }
                        "tbl" -> if (parser.depth == tableDepth) {
                            appendMarkdownTable(output, tableRows)
                            tableDepth = -1
                            tableRows.clear()
                        }
                        "body" -> inBody = false
                    }
                }
            }
            parser.next()
        }

        return output.toString().trim()
    }

    private fun appendMarkdownTable(output: StringBuilder, rows: List<List<String>>) {
        if (rows.isEmpty()) return
        val columns = rows.maxOfOrNull { it.size } ?: return
        if (columns == 0) return
        rows.forEachIndexed { index, row ->
            output.append("| ")
            repeat(columns) { column ->
                val value = row.getOrNull(column).orEmpty()
                    .replace("|", "\\|")
                    .replace("\r", " ")
                    .replace("\n", "<br>")
                output.append(value).append(" | ")
            }
            output.append('\n')
            if (index == 0) {
                output.append("| ")
                repeat(columns) { output.append("--- | ") }
                output.append('\n')
            }
        }
        output.append('\n')
    }

    private fun attribute(parser: XmlPullParser, localName: String): String? {
        for (index in 0 until parser.attributeCount) {
            if (parser.getAttributeName(index) == localName) {
                return parser.getAttributeValue(index)
            }
        }
        return null
    }

    private fun escapeInline(value: String): String = buildString(value.length) {
        value.forEach { char ->
            if (char in "\\\`*_[]") append('\\')
            append(char)
        }
    }
}
