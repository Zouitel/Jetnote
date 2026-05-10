package zouitel.neighborhood.markdown.richtext

import androidx.compose.runtime.Stable
import zouitel.neighborhood.markdown.core.BlockId
import zouitel.neighborhood.markdown.core.SpanStyle
import zouitel.neighborhood.markdown.state.BlockTextStates
import zouitel.neighborhood.markdown.state.EditorStateHolder
import zouitel.neighborhood.markdown.ui.visibleSelection

/**
 * Default implementation of [FormattingActions] that resolves the focused block
 * and visible selection from runtime state **at invocation time** and delegates
 * to [SpanActionDispatcher].
 *
 * No-op when formatting is disallowed (no focus, Code block, block selection
 * active, dragging).
 */
@Stable
class DefaultFormattingActions(
    private val stateHolder: EditorStateHolder,
    private val textStates: BlockTextStates,
    private val spanActionDispatcher: SpanActionDispatcher,
) : FormattingActions {

    override fun toggleStyle(style: SpanStyle) {
        val (blockId, start, end) = resolveContext() ?: return
        spanActionDispatcher.toggleStyle(blockId, start, end, style)
    }

    override fun applyStyle(style: SpanStyle) {
        val (blockId, start, end) = resolveContext() ?: return
        spanActionDispatcher.applyStyle(blockId, start, end, style)
    }

    override fun removeStyle(style: SpanStyle) {
        val (blockId, start, end) = resolveContext() ?: return
        spanActionDispatcher.removeStyle(blockId, start, end, style)
    }

    /**
     * Resolves current formatting context from runtime state.
     * Returns null if formatting is disallowed.
     */
    private fun resolveContext(): FormattingContext? {
        val state = stateHolder.state
        val blockId = state.focusedBlockId ?: return null
        val block = state.getBlock(blockId) ?: return null
        val blockType = block.type

        if (!blockType.supportsText) return null
        if (state.selectedBlockIds.isNotEmpty()) return null
        if (state.dragState != null) return null

        val textFieldState = textStates.get(blockId) ?: return null
        val sel = textFieldState.visibleSelection()

        return FormattingContext(
            blockId = blockId,
            selectionStart = sel.start,
            selectionEnd = sel.end,
        )
    }
}

private data class FormattingContext(
    val blockId: BlockId,
    val selectionStart: Int,
    val selectionEnd: Int,
)
