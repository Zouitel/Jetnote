package zouitel.neighborhood.ai.embedding

/**
 * Describes the exact embedding space used by an embedding provider.
 *
 * A model ID must identify both the model and its revision. Vectors produced by
 * different model revisions must not be compared or mixed in the same index.
 */
data class EmbeddingModel(
    val id: String,
    val dimensions: Int,
    val maxSequenceLength: Int,
    val normalizedOutput: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "Embedding model ID cannot be blank." }
        require(dimensions > 0) { "Embedding dimensions must be positive." }
        require(maxSequenceLength > 0) { "Maximum sequence length must be positive." }
    }
}
