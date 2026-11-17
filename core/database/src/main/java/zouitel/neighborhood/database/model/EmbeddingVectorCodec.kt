package zouitel.neighborhood.database.model

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Stable little-endian encoding for Float32 embedding vectors stored in Room.
 * The model ID and dimensions are stored alongside the bytes in the entity.
 */
object EmbeddingVectorCodec {
    fun encode(values: FloatArray): ByteArray {
        require(values.isNotEmpty()) { "Embedding vector cannot be empty." }
        require(values.all { it.isFinite() }) { "Embedding values must be finite." }
        return ByteBuffer.allocate(values.size * Float.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply { values.forEach(::putFloat) }
            .array()
    }

    fun decode(bytes: ByteArray, dimensions: Int): FloatArray {
        require(dimensions > 0) { "Embedding dimensions must be positive." }
        require(bytes.size == dimensions * Float.SIZE_BYTES) {
            "Stored vector byte count does not match its dimensions."
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(dimensions) {
            buffer.float.also { value ->
                require(value.isFinite()) { "Stored embedding values must be finite." }
            }
        }
    }
}
