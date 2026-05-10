package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.slash.SlashCommandItem

/**
 * Provides the current flat list of slash popup items for keyboard navigation.
 *
 * Computed in [CascadeEditor] from the slash registry search results.
 * Read by [TextBlockField] for Up/Down arrow key navigation.
 */
internal val LocalSlashPopupItems: ProvidableCompositionLocal<List<SlashCommandItem>> =
    compositionLocalOf { emptyList() }
