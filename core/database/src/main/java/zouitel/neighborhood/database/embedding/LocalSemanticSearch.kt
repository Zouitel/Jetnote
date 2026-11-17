package zouitel.neighborhood.database.embedding

import zouitel.neighborhood.ai.embedding.EmbeddingVector
import zouitel.neighborhood.ai.embedding.SemanticSearchCandidate
import zouitel.neighborhood.ai.embedding.SemanticSearchEngine
import zouitel.neighborhood.ai.embedding.SemanticSearchResult
import zouitel.neighborhood.ai.embedding.TextEmbeddingProvider
import zouitel.neighborhood.database.dao.NoteEmbeddingDao
import zouitel.neighborhood.database.model.EmbeddingVectorCodec

/**
 * Searches only the active user's locally persisted vectors. Optional lexical
 * scores can be supplied by the existing FTS search path for hybrid ranking.
 */
class LocalSemanticSearch(
    private val embeddings: NoteEmbeddingDao,
    private val provider: TextEmbeddingProvider,
    private val engine: SemanticSearchEngine = SemanticSearchEngine(),
) {
    suspend fun search(
        userUid: String,
        query: String,
        limit: Int = 20,
        minScore: Double = 0.55,
        lexicalScores: Map<String, Double> = emptyMap(),
    ): List<SemanticSearchResult> {
        require(userUid.isNotBlank()) { "User UID must not be blank." }
        require(query.isNotBlank()) { "Search query must not be blank." }

        val model = provider.model
        val queryVector = provider.embedQuery(query)
        check(queryVector.modelId == model.id) {
            "Embedding provider returned a query vector for an unexpected model."
        }
        check(queryVector.dimensions == model.dimensions) {
            "Embedding provider returned an unexpected query vector dimension."
        }

        val candidates = embeddings.getActiveForUser(userUid, model.id).map { stored ->
            val vector = EmbeddingVector(
                modelId = stored.modelId,
                values = EmbeddingVectorCodec.decode(stored.vector, stored.dimensions),
            )
            SemanticSearchCandidate(
                noteId = stored.noteUid,
                embedding = vector,
                lexicalScore = lexicalScores[stored.noteUid] ?: 0.0,
            )
        }
        return engine.search(
            query = queryVector,
            candidates = candidates,
            limit = limit,
            minScore = minScore,
        )
    }
}
