package zouitel.neighborhood.assistant.ai.transformers

import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import io.pebbletemplates.pebble.PebbleEngine
import zouitel.neighborhood.assistant.utils.toLocalDate
import zouitel.neighborhood.assistant.utils.toLocalTime
import io.pebbletemplates.pebble.loader.Loader
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter
import java.time.Instant

class TemplateTransformer(
    private val engine: PebbleEngine
) : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val template = engine.getTemplate(ctx.assistant?.id.toString())
        return messages.map { message ->
            message.copy(
                parts = message.parts.map { part ->
                    when (part) {
                        is UIMessagePart.Text -> {
                            val result = StringWriter()
                            template.evaluate(
                                result, mapOf(
                                    "message" to part.text,
                                    "role" to message.role.name.lowercase(),
                                    "time" to Instant.now().toLocalTime(),
                                    "date" to Instant.now().toLocalDate(),
                                )
                            )
                            part.copy(
                                text = result.toString()
                            )
                        }

                        else -> part
                    }
                }
            )
        }
    }
}

class AssistantTemplateLoader() : Loader<String> {
    override fun getReader(cacheKey: String?): Reader? {
//        val content = settingsStore.settingsFlow.value.assistants
//            .find { it.uid.toString() == cacheKey }?.messageTemplate
//            ?: return null
        return StringReader("{{message}}")
    }

    override fun setCharset(charset: String?) {}

    override fun setPrefix(prefix: String?) {}

    override fun setSuffix(suffix: String?) {}

    override fun resolveRelativePath(
        relativePath: String?,
        anchorPath: String?
    ): String? {
        return relativePath
    }

    override fun createCacheKey(templateName: String?): String? {
        return templateName
    }

    override fun resourceExists(templateName: String?): Boolean {
        return true
    }
}