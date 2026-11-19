package zouitel.neighborhood.markdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.markdown.action.ConvertBlockType
import zouitel.neighborhood.markdown.action.FocusBlock
import zouitel.neighborhood.markdown.action.InsertBlockAfter
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.markdown.state.EditorStateHolder
import zouitel.neighborhood.markdown.theme.LocalCascadeTheme

/**
 * Compact block toolbar: history controls followed by direct block-type actions.
 * The row stays on one line and scrolls horizontally on narrow screens.
 */
@Composable
internal fun BlockActionToolbar(
    stateHolder: EditorStateHolder,
    modifier: Modifier = Modifier,
) {
    val state = stateHolder.state
    val theme = LocalCascadeTheme.current
    val focusedBlock = state.focusedBlockId?.let(state::getBlock)
    val enabled = focusedBlock != null && state.selectedBlockIds.isEmpty()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusProperties { canFocus = false }
            .background(theme.colors.toolbarBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BlockToolbarButton("↶", "Undo", stateHolder.canUndo, theme.colors.toolbarIcon) {
            stateHolder.undo()
        }
        BlockToolbarButton("↷", "Redo", stateHolder.canRedo, theme.colors.toolbarIcon) {
            stateHolder.redo()
        }
        BlockToolbarButton("P", "Paragraph", enabled, theme.colors.toolbarIcon,
            active = focusedBlock?.type == BlockType.Paragraph) {
            convertFocused(stateHolder, BlockType.Paragraph)
        }
        (1..6).forEach { level ->
            val type = BlockType.Heading(level)
            BlockToolbarButton("H$level", "Heading $level", enabled, theme.colors.toolbarIcon,
                active = focusedBlock?.type == type) {
                convertFocused(stateHolder, type)
            }
        }
        BlockToolbarButton("☑", "To-do", enabled, theme.colors.toolbarIcon,
            active = focusedBlock?.type is BlockType.Todo) {
            convertFocused(stateHolder, BlockType.Todo())
        }
        BlockToolbarButton("•", "Bulleted list", enabled, theme.colors.toolbarIcon,
            active = focusedBlock?.type == BlockType.BulletList) {
            convertFocused(stateHolder, BlockType.BulletList)
        }
        BlockToolbarButton("1.", "Numbered list", enabled, theme.colors.toolbarIcon,
            active = focusedBlock?.type is BlockType.NumberedList) {
            convertFocused(stateHolder, BlockType.NumberedList())
        }
        BlockToolbarButton("❝", "Quote", enabled, theme.colors.toolbarIcon,
            active = focusedBlock?.type == BlockType.Quote) {
            convertFocused(stateHolder, BlockType.Quote)
        }
        BlockToolbarButton("―", "Insert divider", enabled, theme.colors.toolbarIcon) {
            val anchorId = stateHolder.state.focusedBlockId ?: return@BlockToolbarButton
            stateHolder.runStructuralHistoryTransaction {
                val divider = Block.divider()
                val nextParagraph = Block.paragraph()
                stateHolder.dispatch(InsertBlockAfter(divider, anchorId))
                stateHolder.dispatch(InsertBlockAfter(nextParagraph, divider.id))
                stateHolder.dispatch(FocusBlock(nextParagraph.id))
            }
        }
    }
}

@Composable
private fun BlockToolbarButton(
    label: String,
    description: String,
    enabled: Boolean,
    iconColor: Color,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalCascadeTheme.current.colors
    val background = when {
        active -> colors.primary
        else -> Color.Transparent
    }
    val foreground = when {
        !enabled -> colors.toolbarIconDisabled
        active -> colors.onPrimary
        else -> iconColor
    }
    Row(
        modifier = Modifier
            .sizeIn(minWidth = 42.dp, minHeight = 42.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(background)
            .nonFocusableTap(enabled, onClick)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = LocalCascadeTheme.current.typography.toolbarButton.copy(color = foreground),
        )
    }
}

private fun convertFocused(
    stateHolder: EditorStateHolder,
    type: BlockType,
) {
    val blockId = stateHolder.state.focusedBlockId ?: return
    stateHolder.runStructuralHistoryTransaction {
        stateHolder.dispatch(ConvertBlockType(blockId, type))
    }
}
