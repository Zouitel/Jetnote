package zouitel.neighborhood.markdown.core

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable                                          // ← ADD
sealed interface SpanStyle {

    @Serializable                                      // ← ADD
    data object Bold : SpanStyle

    @Serializable                                      // ← ADD
    data object Italic : SpanStyle

    @Serializable                                      // ← ADD
    data object Underline : SpanStyle

    @Serializable                                      // ← ADD
    data object StrikeThrough : SpanStyle

    @Serializable                                      // ← ADD
    data object InlineCode : SpanStyle

    @Serializable                                      // ← ADD
    data class Highlight(val colorArgb: Long) : SpanStyle

    @Serializable                                      // ← ADD
    data class Custom(
        val customId: String,
        val payload: String? = null,
    ) : SpanStyle

    companion object {
        fun kindKey(style: SpanStyle): Any = when (style) {
            is Bold         -> Bold
            is Italic       -> Italic
            is Underline    -> Underline
            is StrikeThrough -> StrikeThrough
            is InlineCode   -> InlineCode
            is Highlight    -> Highlight::class
            is Custom       -> style
        }

        fun kindMatches(a: SpanStyle, b: SpanStyle): Boolean =
            kindKey(a) == kindKey(b)
    }
}