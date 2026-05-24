package zouitel.neighborhood.search.state

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import zouitel.neighborhood.search.utils.AppScreen
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.workspace.model.Data

/**
 * Immutable UI state for the search screen.
 *
 * [TextFieldState] has been removed from this class deliberately.
 * It is a Compose mutable-state object — putting it in a data class breaks
 * equals/copy, and StateFlow only notifies collectors when the *reference*
 * changes, so text typed by the user would never propagate through the flow.
 * The field state now lives directly on [SearchScreenModel] as a stable property.
 */
data class UiState(

    /**
     * Active tag-label filter.  Empty string = no tag filter.
     * Set by [SearchScreenModel.updateSearchTag].
     */
    val tag: String = DEFAULT_TXT,

    /** Whether the multi-select mode is active. */
    val isSelection: Boolean = false,

    val expandedSortMenu: Boolean = false,
    val isProcessing: Boolean = false,
    val isErase: Boolean = false,
    val isEraseDialog: Boolean = false,
    val isOptionsDialog: Boolean = false,

    /** Notes currently selected by the user (observed by Compose individually). */
    val selectedNotes: SnapshotStateList<Data> = mutableStateListOf(),

    /** UID of the note shown in a context-menu / action sheet. */
    val selectedNote: String = "",

    /**
     * The active search dimension.
     *
     * [SearchTypes.Text] = free-text search (uses [SearchScreenModel.fieldState]).
     * All other types = category filter (field text is only a display label).
     */
    val typeState: SearchTypes = SearchTypes.Text(""),

    /** Which top-level screen is active (used by the navigation drawer). */
    val currentScreen: AppScreen = AppScreen.MAIN_SCREEN,

    /** Persisted image-search path across recompositions. */
    val searchImage: SearchTypes = SearchTypes.Image(""),
) {
    @SuppressLint("SuspiciousIndentation")
    fun addSelectedNote(note: Data): UiState {
        if (!selectedNotes.contains(note))
            selectedNotes.add(note)
        return this
    }
}