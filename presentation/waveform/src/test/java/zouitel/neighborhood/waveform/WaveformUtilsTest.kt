package zouitel.neighborhood.waveform

import org.junit.Assert.assertEquals
import org.junit.Test

class WaveformUtilsTest {

    @Test
    fun fillToSizeReturnsRequestedNumberOfValues() {
        val result = listOf(2, 4, 6).fillToSize(5) { bucket -> bucket.sum() }

        assertEquals(5, result.size)
    }

    @Test
    fun fillToSizeHandlesEmptyInput() {
        assertEquals(emptyList<Int>(), emptyList<Int>().fillToSize(4) { it.sum() })
    }

    @Test
    fun chunkToSizeAggregatesEvenBuckets() {
        val result = (1..6).toList().chunkToSize(3) { bucket -> bucket.sum() }

        assertEquals(listOf(3, 7, 11), result)
    }

    @Test
    fun chunkToSizeHandlesNonPositiveTargetSize() {
        assertEquals(emptyList<Int>(), listOf(1, 2, 3).chunkToSize(0) { it.sum() })
    }

    @Test
    fun normalizeMapsConstantValuesToMinimum() {
        assertEquals(listOf(1F, 1F, 1F), listOf(5F, 5F, 5F).normalize(1F, 10F))
    }

    @Test
    fun normalizePreservesRelativeOrdering() {
        val result = listOf(2F, 4F, 6F).normalize(1F, 9F)

        assertEquals(listOf(1F, 5F, 9F), result)
    }
}
