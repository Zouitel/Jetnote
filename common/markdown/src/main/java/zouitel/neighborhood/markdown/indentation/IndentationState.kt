package zouitel.neighborhood.markdown.indentation

import androidx.compose.runtime.Immutable
import zouitel.neighborhood.markdown.core.BlockId

/**
 * Immutable indentation command state for the current focus or block selection.
 *
 * [targetBlockIds] is ordered for consumers that mirror the command target in
 * document chrome. It contains only supported root targets, not descendants that
 * would move as part of those roots' flat-outline subtrees.
 */
@Immutable
 data class IndentationState(
    val canIndentForward: Boolean,
    val canIndentBackward: Boolean,
    val targetBlockIds: List<BlockId>,
) {
     companion object {
        /**
         * State used when no supported indentation target is available.
         */
         val Empty: IndentationState = IndentationState(
            canIndentForward = false,
            canIndentBackward = false,
            targetBlockIds = emptyList(),
        )
    }
}
