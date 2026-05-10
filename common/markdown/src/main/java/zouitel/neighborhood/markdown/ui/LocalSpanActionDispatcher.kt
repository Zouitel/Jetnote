package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.richtext.SpanActionDispatcher

/**
 * Provides [SpanActionDispatcher] to composables for rich text formatting operations.
 *
 * Returns `null` if no dispatcher has been provided (e.g., outside of [CascadeEditor]).
 */
val LocalSpanActionDispatcher: ProvidableCompositionLocal<SpanActionDispatcher?> =
    compositionLocalOf { null }
