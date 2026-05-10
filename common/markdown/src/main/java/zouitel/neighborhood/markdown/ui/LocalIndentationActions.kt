package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.indentation.IndentationActions

/**
 * Provides [IndentationActions] to custom editor chrome.
 *
 * Returns `null` outside of [CascadeEditor].
 */
val LocalIndentationActions: ProvidableCompositionLocal<IndentationActions?> =
    compositionLocalOf { null }
