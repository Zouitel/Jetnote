package zouitel.neighborhood.markdown.ui.renderers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.markdown.registry.BlockCallbacks
import zouitel.neighborhood.markdown.registry.BlockRenderer
import zouitel.neighborhood.markdown.theme.LocalCascadeTheme
import zouitel.neighborhood.markdown.ui.utils.Dividers

/**
 * Renderer for Divider blocks — a horizontal line separator.
 */
 class DividerBlockRenderer : BlockRenderer<BlockType.Divider> {

    @Composable
    override fun Render(
        block: Block,
        isSelected: Boolean,
        isFocused: Boolean,
        modifier: Modifier,
        callbacks: BlockCallbacks
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Dividers.Horizontal(color = LocalCascadeTheme.current.colors.contentDivider)
        }
    }
}
