package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import zouitel.neighborhood.markdown.slash.SlashCommandExecutor

/**
 * Provides the editor-owned slash execution coordinator.
 *
 * This remains local to the UI layer so text fields and future slash UI
 * components can trigger command execution without owning coordinator state.
 */
internal val LocalSlashCommandExecutor: ProvidableCompositionLocal<SlashCommandExecutor?> =
    compositionLocalOf { null }
