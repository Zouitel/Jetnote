package zouitel.neighborhood.search.state

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import zouitel.neighborhood.search.utils.AppScreen
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.workspace.model.Data

data class UiState(
    val tag: String = "",
    val isSelection: Boolean = false,
    val expandedSortMenu: Boolean = false,
    val isProcessing: Boolean = false,
    val isErase: Boolean = false,
    val isEraseDialog: Boolean = false,
    val isOptionsDialog: Boolean = false,
    val selectedNotes: SnapshotStateList<Data> = mutableStateListOf(),
    val selectedNote: String = "",
    val typeState: SearchTypes = SearchTypes.Text(""),
    val currentScreen: AppScreen = AppScreen.MAIN_SCREEN,
    val searchImage: SearchTypes = SearchTypes.Image("")
) {
    @SuppressLint("SuspiciousIndentation")
    fun addSelectedNote(note: Data): UiState {
        if (!selectedNotes.contains(note))
            selectedNotes.add(note)
        return this
    }
}