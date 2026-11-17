package zouitel.neighborhood.ai.embedding

/**
 * A note candidate supplied by the local index.
 *
 * [lexicalScore] is an optional non-negative relevance score from the existing
 * FTS index. It is kept separate from the embedding so hybrid ranking can
 * combine both signals without changing the FTS implementation.
 */
data class SemanticSearchCandidate(
    val noteId: String,
    val embedding: EmbeddingVector,
    val lexicalScore: Double = 0.0,
) {
    init {
        require(noteId.isNotBlank()) { "Note ID cannot be blank." }
        require(lexicalScore.isFinite() && lexicalScore >= 0.0) {
            "Lexical score must be finite and non-negative."
        }
    }
}

/** A ranked note result with its component scores available for diagnostics/UI. */
data class SemanticSearchResult(
    val noteId: String,
    val score: Double,
    val semanticScore: Double,
    val lexicalScore: Double,
)
