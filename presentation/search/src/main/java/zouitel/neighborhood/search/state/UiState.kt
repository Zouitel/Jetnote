package zouitel.neighborhood.search.state

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import zouitel.neighborhood.search.utils.AppScreen
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.search.utils.SearchTypes.*

data class UiState(
    val text: String = DEFAULT_TXT,
    val tag: String = DEFAULT_TXT,
    val searchTitle: String = "",
    val searchTag: Tag? = null,
    val expandedSortMenu: Boolean = false,
    val isProcessing: Boolean = false,
    val isSelection: Boolean = false,
    val isErase: Boolean = false,
    val isEraseDialog: Boolean = false,
    val isOptionsDialog: Boolean = false,
    val selectedNotes: SnapshotStateList<Data> = mutableStateListOf(),
    val selectedNote: String = "",
    val typeState: SearchTypes = Text(DEFAULT_TXT),
    val currentScreen: AppScreen = AppScreen.MAIN_SCREEN,
    val searchImage: SearchTypes = Image(""),
) {
    @SuppressLint("SuspiciousIndentation")
    fun addSelectedNote(note: Data): UiState {
        if (selectedNotes.contains(note).not())
        selectedNotes.add(note)
        return this
    }
}
