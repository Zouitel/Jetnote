package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import zouitel.neighborhood.markdown.richtext.FormattingState

/**
 * Provides reactive [FormattingState] to custom editor chrome and standalone toolbar.
 *
 * Always provided by [CascadeEditor]. Returns `null` outside of [CascadeEditor].
 */
public val LocalFormattingState: ProvidableCompositionLocal<State<FormattingState>?> =
    staticCompositionLocalOf { null }