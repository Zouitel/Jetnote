package zouitel.neighborhood.database.model

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddingVectorCodecTest {
    @Test
    fun encodesAndDecodesFloatVectors() {
        val values = floatArrayOf(1f, -0.25f, 0.5f)

        val decoded = EmbeddingVectorCodec.decode(
            EmbeddingVectorCodec.encode(values),
            dimensions = values.size,
        )

        assertArrayEquals(values, decoded, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDimensionMismatch() {
        EmbeddingVectorCodec.decode(byteArrayOf(0, 0, 0, 0), dimensions = 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonFiniteValues() {
        EmbeddingVectorCodec.encode(floatArrayOf(1f, Float.NaN))
    }
}
