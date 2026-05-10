package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import zouitel.neighborhood.markdown.indentation.IndentationState

/**
 * Provides reactive [IndentationState] for custom editor chrome.
 *
 * Custom toolbars can read this CompositionLocal instead of requiring a new
 * [ToolbarSlot.Custom] lambda parameter.
 */
val LocalIndentationState: ProvidableCompositionLocal<State<IndentationState>?> =
    staticCompositionLocalOf { null }
