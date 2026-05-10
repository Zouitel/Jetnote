package zouitel.neighborhood.markdown.ui.renderers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.UnknownBlockType
import zouitel.neighborhood.markdown.registry.BlockCallbacks
import zouitel.neighborhood.markdown.registry.BlockRenderer
import zouitel.neighborhood.markdown.theme.LocalCascadeStrings
import zouitel.neighborhood.markdown.theme.LocalCascadeTheme

/**
 * Placeholder renderer for [UnknownBlockType] blocks.
 *
 * Displays a muted box with the unrecognized type ID. Non-focusable, non-editable.
 * Registered as the [BlockRegistry][zouitel.neighborhood.markdown.registry.BlockRegistry]
 * unknown-block fallback via [setUnknownBlockRenderer][zouitel.neighborhood.markdown.registry.BlockRegistry.setUnknownBlockRenderer].
 */
internal object UnknownBlockRenderer : BlockRenderer<UnknownBlockType> {

    @Composable
    override fun Render(
        block: Block,
        isSelected: Boolean,
        isFocused: Boolean,
        modifier: Modifier,
        callbacks: BlockCallbacks,
    ) {
        val colors = LocalCascadeTheme.current.colors
        val strings = LocalCascadeStrings.current
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = colors.unknownBlockBackground,
                    shape = RoundedCornerShape(4.dp),
                )
                .padding(12.dp),
        ) {
            BasicText(
                text = strings.unsupportedBlock(block.type.typeId),
                style = TextStyle(
                    color = colors.unknownBlockText,
                    fontSize = 14.sp,
                ),
            )
        }
    }
}
