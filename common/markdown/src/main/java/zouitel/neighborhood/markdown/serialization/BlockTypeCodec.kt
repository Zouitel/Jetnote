package zouitel.neighborhood.markdown.serialization

import zouitel.neighborhood.markdown.core.BlockType
import kotlinx.serialization.json.JsonObject

/**
 * Consumer-provided codec for encoding/decoding custom [BlockType] instances.
 *
 * Return `null` to fall through to the default encoding/decoding logic.
 */
interface BlockTypeCodec {
    /** Encode a [BlockType] to JSON. Return `null` to use default encoding. */
    fun encodeType(type: BlockType): JsonObject?

    /** Decode a [BlockType] from JSON by its [typeId]. Return `null` to use default decoding. */
    fun decodeType(typeId: String, json: JsonObject): BlockType?
}
