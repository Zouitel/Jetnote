package zouitel.neighborhood.navigator.trash_screen

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.navigator.mapper.NoteMapper
import zouitel.neighborhood.workspace.model.Note

@Stable
class TrashScreenModel(
    private val observeTrash: NoteUseCase.ObserveTrash,
    private val restore: DataUseCase.RestoreFromTrash,
    private val permanentDelete: DataUseCase.PermanentDelete,
    private val emptyTrash: DataUseCase.EmptyTrash,
    private val mapper: NoteMapper,
    private val auth: FirebaseAuth
) : ScreenModel {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun observe() {
        screenModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                // The screen is also refreshed by initialize() when account changes.
            }
            auth.currentUser?.uid?.let { uid ->
                observeTrash(uid).collectLatest { values ->
                    _notes.value = mapper.fromDomain(values)
                }
            } ?: run { _notes.value = emptyList() }
        }
    }

    fun restore(uid: String) {
        screenModelScope.launch { restore.invoke(uid) }
    }

    fun deletePermanently(uid: String) {
        screenModelScope.launch { permanentDelete(uid) }
    }

    fun empty() {
        screenModelScope.launch { emptyTrash() }
    }
}
