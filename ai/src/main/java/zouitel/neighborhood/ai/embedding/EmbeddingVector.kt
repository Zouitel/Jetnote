package zouitel.neighborhood.ai.embedding

/**
 * Immutable, model-tagged vector returned by [TextEmbeddingProvider].
 *
 * The input array is copied on construction and when read so callers cannot
 * mutate vectors after they have been indexed or compared.
 */
class EmbeddingVector(
    val modelId: String,
    values: FloatArray,
) {
    private val components = values.copyOf()

    val dimensions: Int
        get() = components.size

    init {
        require(modelId.isNotBlank()) { "Embedding model ID cannot be blank." }
        require(components.isNotEmpty()) { "An embedding vector cannot be empty." }
        require(components.all { it.isFinite() }) { "Embedding values must be finite." }
    }

    operator fun get(index: Int): Float = components[index]

    fun toFloatArray(): FloatArray = components.copyOf()

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is EmbeddingVector &&
                modelId == other.modelId &&
                components.contentEquals(other.components)
            )

    override fun hashCode(): Int = 31 * modelId.hashCode() + components.contentHashCode()

    override fun toString(): String =
        "EmbeddingVector(modelId=$modelId, dimensions=$dimensions)"
}
