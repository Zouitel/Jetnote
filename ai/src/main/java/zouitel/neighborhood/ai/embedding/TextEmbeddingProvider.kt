package zouitel.neighborhood.ai.embedding

/**
 * Generates embeddings using one explicitly identified embedding model.
 *
 * Implementations should run inference locally and must not send note content
 * to a remote service. Query and document methods allow asymmetric retrieval
 * models to apply their required task instructions.
 */
interface TextEmbeddingProvider {
    val model: EmbeddingModel

    /** Generic embedding operation retained for symmetric embedding models. */
    suspend fun embed(text: String): EmbeddingVector

    /** Embeds a stored document/passsage. */
    suspend fun embedDocument(text: String): EmbeddingVector = embed(text)

    /** Embeds a user search query. */
    suspend fun embedQuery(text: String): EmbeddingVector = embed(text)

    /**
     * Embeds documents in input order. Runtime-backed providers can override
     * this method to use native batching; the default remains model-agnostic.
     */
    suspend fun embedAll(texts: List<String>): List<EmbeddingVector> =
        texts.map { embedDocument(it) }
}
