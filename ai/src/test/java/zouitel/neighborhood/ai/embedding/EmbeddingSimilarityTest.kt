package zouitel.neighborhood.ai.embedding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EmbeddingSimilarityTest {

    @Test
    fun cosineReturnsOneForIdenticalVectors() {
        val vector = embedding(1f, 2f, 3f)

        assertEquals(1f, EmbeddingSimilarity.cosine(vector, vector), 0.0001f)
    }

    @Test
    fun cosineReturnsZeroForOrthogonalVectors() {
        val left = embedding(1f, 0f)
        val right = embedding(0f, 1f)

        assertEquals(0f, EmbeddingSimilarity.cosine(left, right), 0.0001f)
    }

    @Test
    fun cosineReturnsMinusOneForOppositeVectors() {
        val left = embedding(1f, 0f)
        val right = embedding(-1f, 0f)

        assertEquals(-1f, EmbeddingSimilarity.cosine(left, right), 0.0001f)
    }

    @Test
    fun cosineReturnsZeroWhenEitherVectorHasZeroMagnitude() {
        val left = embedding(0f, 0f)
        val right = embedding(1f, 2f)

        assertEquals(0f, EmbeddingSimilarity.cosine(left, right), 0f)
    }

    @Test
    fun cosineRejectsDifferentModelRevisions() {
        val left = EmbeddingVector("model-v1", floatArrayOf(1f, 0f))
        val right = EmbeddingVector("model-v2", floatArrayOf(1f, 0f))

        assertThrows(IllegalArgumentException::class.java) {
            EmbeddingSimilarity.cosine(left, right)
        }
    }

    @Test
    fun vectorDefensivelyCopiesValues() {
        val source = floatArrayOf(1f, 2f)
        val vector = EmbeddingVector("test-model", source)
        source[0] = 99f
        val exported = vector.toFloatArray()
        exported[1] = 99f

        assertEquals(1f, vector[0], 0f)
        assertEquals(2f, vector[1], 0f)
    }

    private fun embedding(vararg values: Float) =
        EmbeddingVector("test-model-v1", values)
}
