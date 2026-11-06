package zouitel.neighborhood.markdown.richtext

import org.junit.Assert.assertEquals
import org.junit.Test
import zouitel.neighborhood.markdown.core.SpanStyle
import zouitel.neighborhood.markdown.core.TextSpan

class SpanAlgorithmsTest {

    @Test
    fun removingHighlightPreservesOriginalColorOnBothRemainingFragments() {
        val yellow = SpanStyle.Highlight(0xFFFFFF00)
        val blue = SpanStyle.Highlight(0xFF0000FF)

        val result = SpanAlgorithms.removeStyle(
            spans = listOf(TextSpan(start = 0, end = 10, style = yellow)),
            rangeStart = 3,
            rangeEnd = 7,
            style = blue,
        )

        assertEquals(
            listOf(
                TextSpan(start = 0, end = 3, style = yellow),
                TextSpan(start = 7, end = 10, style = yellow),
            ),
            result,
        )
    }

    @Test
    fun removingHighlightDoesNotChangeColorOfUnrelatedSpans() {
        val yellow = SpanStyle.Highlight(0xFFFFFF00)
        val green = SpanStyle.Highlight(0xFF00FF00)
        val blue = SpanStyle.Highlight(0xFF0000FF)

        val result = SpanAlgorithms.removeStyle(
            spans = listOf(
                TextSpan(start = 0, end = 4, style = yellow),
                TextSpan(start = 6, end = 10, style = green),
            ),
            rangeStart = 4,
            rangeEnd = 6,
            style = blue,
        )

        assertEquals(
            listOf(
                TextSpan(start = 0, end = 4, style = yellow),
                TextSpan(start = 6, end = 10, style = green),
            ),
            result,
        )
    }
}
