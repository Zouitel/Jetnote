package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.slash.SlashCommandId

/**
 * Provides the currently highlighted slash command id, if any.
 */
internal val LocalSlashHighlightedCommandId: ProvidableCompositionLocal<SlashCommandId?> =
    compositionLocalOf { null }
