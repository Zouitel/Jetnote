package zouitel.neighborhood.ai.embedding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticSearchEngineTest {
    private val engine = SemanticSearchEngine()

    @Test
    fun ranksByMeaningAndReturnsHighestFirst() {
        val query = EmbeddingVector("model-v1", floatArrayOf(1f, 0f))
        val results = engine.search(
            query = query,
            candidates = listOf(
                SemanticSearchCandidate("unrelated", EmbeddingVector("model-v1", floatArrayOf(0f, 1f))),
                SemanticSearchCandidate("related", EmbeddingVector("model-v1", floatArrayOf(0.9f, 0.1f))),
            ),
        )

        assertEquals("related", results.first().noteId)
        assertTrue(results.first().score > results.last().score)
    }

    @Test
    fun combinesSemanticAndLexicalScores() {
        val query = EmbeddingVector("model-v1", floatArrayOf(1f, 0f))
        val results = engine.search(
            query = query,
            candidates = listOf(
                SemanticSearchCandidate(
                    "semantic",
                    EmbeddingVector("model-v1", floatArrayOf(1f, 0f)),
                    lexicalScore = 0.0,
                ),
                SemanticSearchCandidate(
                    "lexical",
                    EmbeddingVector("model-v1", floatArrayOf(0f, 1f)),
                    lexicalScore = 10.0,
                ),
            ),
            semanticWeight = 0.5,
        )

        assertEquals("semantic", results.first().noteId)
        assertEquals(1.0, results.first().semanticScore, 0.0001)
        assertEquals(1.0, results.last().lexicalScore, 0.0001)
    }

    @Test
    fun appliesThresholdAndLimit() {
        val query = EmbeddingVector("model-v1", floatArrayOf(1f, 0f))
        val results = engine.search(
            query = query,
            candidates = listOf(
                SemanticSearchCandidate("first", EmbeddingVector("model-v1", floatArrayOf(1f, 0f))),
                SemanticSearchCandidate("second", EmbeddingVector("model-v1", floatArrayOf(0f, 1f))),
            ),
            limit = 1,
            minScore = 0.9,
        )

        assertEquals(listOf("first"), results.map { it.noteId })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCandidatesFromAnotherModel() {
        engine.search(
            query = EmbeddingVector("model-v1", floatArrayOf(1f, 0f)),
            candidates = listOf(
                SemanticSearchCandidate("wrong-model", EmbeddingVector("model-v2", floatArrayOf(1f, 0f))),
            ),
        )
    }

    @Test
    fun returnsEmptyForNoCandidates() {
        assertTrue(
            engine.search(
                EmbeddingVector("model-v1", floatArrayOf(1f, 0f)),
                emptyList(),
            ).isEmpty()
        )
    }
}
