package zouitel.neighborhood.markdown.serialization

import zouitel.neighborhood.markdown.core.BlockContent
import kotlinx.serialization.json.JsonObject

/**
 * Consumer-provided codec for encoding/decoding custom [BlockContent] instances.
 *
 * Return `null` to fall through to the default encoding/decoding logic.
 */
interface BlockContentCodec {
    /** Encode [BlockContent] to JSON. Return `null` to use default encoding. */
    fun encodeContent(content: BlockContent): JsonObject?

    /** Decode [BlockContent] from JSON by its [kind]. Return `null` to use default decoding. */
    fun decodeContent(kind: String, json: JsonObject): BlockContent?
}
