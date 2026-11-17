package zouitel.neighborhood.ai.embedding

/**
 * Generates text embeddings using one explicitly identified embedding model.
 *
 * Implementations should run inference locally and must not send note content
 * to a remote service. The interface is deliberately separate from chat/image
 * [zouitel.neighborhood.ai.provider.Provider] implementations: generation and
 * embedding models have different input, output, and lifecycle requirements.
 */
interface TextEmbeddingProvider {
    val model: EmbeddingModel

    suspend fun embed(text: String): EmbeddingVector

    /**
     * Embeds texts in input order. Runtime-backed providers can override this
     * method to use native batching; the default keeps implementations simple.
     */
    suspend fun embedAll(texts: List<String>): List<EmbeddingVector> =
        texts.map { embed(it) }
}
