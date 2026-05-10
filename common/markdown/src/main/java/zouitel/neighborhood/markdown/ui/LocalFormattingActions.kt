package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.richtext.FormattingActions

/**
 * Provides [FormattingActions] to composables for rich text formatting operations
 * (toolbar buttons, keyboard shortcuts).
 *
 * Returns `null` if no actions have been provided (e.g., outside of [CascadeEditor]).
 */
val LocalFormattingActions: ProvidableCompositionLocal<FormattingActions?> =
    compositionLocalOf { null }
