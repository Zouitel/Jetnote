package zouitel.neighborhood.database.embedding

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Extracts searchable plain text from the editor's serialized block document.
 * Only text payloads are indexed; block metadata, IDs, styles and custom JSON
 * are not treated as natural-language content.
 */
internal object NoteEmbeddingText {
    private val json = Json { ignoreUnknownKeys = true }

    fun from(title: String, serializedBlocks: String): String {
        val textParts = buildList {
            title.trim().takeIf(String::isNotEmpty)?.let(::add)
            val document = runCatching { json.parseToJsonElement(serializedBlocks) }
                .getOrNull()
            if (document != null) collectText(document, this)
            else serializedBlocks.trim().takeIf(String::isNotEmpty)?.let(::add)
        }
        return textParts.joinToString(separator = "\n")
    }

    private fun collectText(element: JsonElement, output: MutableList<String>) {
        when (element) {
            is JsonArray -> element.forEach { collectText(it, output) }
            is JsonObject -> element.forEach { (key, value) ->
                if (key == "text" && value is JsonPrimitive && value.isString) {
                    value.content.trim().takeIf(String::isNotEmpty)?.let(output::add)
                } else {
                    collectText(value, output)
                }
            }
            else -> Unit
        }
    }
}
