package zouitel.neighborhood.assistant.ai.transformers

import androidx.core.net.toFile
import androidx.core.net.toUri
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.document.DocxParser
import zouitel.neighborhood.document.PdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DocumentAsPromptTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return withContext(Dispatchers.IO) {
            messages.map { message ->
                message.copy(
                    parts = message.parts.toMutableList().apply {
                        val documents = filterIsInstance<UIMessagePart.Document>()
                        if (documents.isNotEmpty()) {
                            documents.forEach { document ->
                                val file = document.url.toUri().toFile()
                                val content = when (document.mime) {
                                    "application/pdf" -> parsePdfAsText(file)
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocxAsText(
                                        file
                                    )

                                    else -> file.readText()
                                }
                                val prompt = """
                  ## user sent a file: ${document.fileName}
                  <content>
                  ```
                  $content
                  ```
                  </content>
                  """.trimMargin()
                                add(0, UIMessagePart.Text(prompt))
                            }
                        }
                    }
                )
            }
        }
    }

    private fun parsePdfAsText(file: File): String {
        return PdfParser.parserPdf(file)
    }

    private fun parseDocxAsText(file: File): String {
        return DocxParser.parse(file)
    }
}
