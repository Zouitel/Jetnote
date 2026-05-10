package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.state.BlockTextStates

/**
 * Provides [BlockTextStates] to text-capable block renderers.
 *
 * This CompositionLocal allows renderers to access the shared text state
 * manager without explicit parameter passing through the entire hierarchy.
 *
 * Usage in renderers:
 * ```
 * val blockTextStates = LocalBlockTextStates.current
 * val textFieldState = blockTextStates.getOrCreate(block.id, initialText)
 * ```
 */
val LocalBlockTextStates: ProvidableCompositionLocal<BlockTextStates> = compositionLocalOf<BlockTextStates> {
    error("BlockTextStates not provided. Ensure CascadeEditor is properly initialized.")
}
