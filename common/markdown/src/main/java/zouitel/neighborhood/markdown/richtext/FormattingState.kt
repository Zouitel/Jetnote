package zouitel.neighborhood.markdown.richtext

import androidx.compose.runtime.Immutable
import zouitel.neighborhood.markdown.core.BlockId
import zouitel.neighborhood.markdown.core.SpanStyle

/**
 * Immutable snapshot of the current formatting state for the focused text block.
 * Consumed by toolbar UI and external formatting state listeners.
 */
@Immutable
 data class FormattingState(
    val styles: Map<SpanStyle, StyleStatus>,
    val canFormat: Boolean,
    val focusedBlockId: BlockId?,
    val selectionCollapsed: Boolean,
) {
     fun styleStatusOf(style: SpanStyle): StyleStatus =
        styles[style] ?: StyleStatus.Absent

     companion object {
         val Empty: FormattingState = FormattingState(
            styles = emptyMap(),
            canFormat = false,
            focusedBlockId = null,
            selectionCollapsed = true,
        )
    }
}
