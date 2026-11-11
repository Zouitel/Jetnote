package zouitel.neighborhood.navigator.trash_screen

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.navigator.mapper.NoteMapper
import zouitel.neighborhood.workspace.model.Note

@Stable
class TrashScreenModel(
    private val observeTrash: NoteUseCase.ObserveTrash,
    private val restoreNote: DataUseCase.RestoreFromTrash,
    private val deleteNotePermanently: DataUseCase.PermanentDelete,
    private val emptyUserTrash: DataUseCase.EmptyTrash,
    private val mapper: NoteMapper,
    private val auth: FirebaseAuth
) : ScreenModel {
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()

    fun observe() {
        screenModelScope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    trySend(firebaseAuth.currentUser?.uid)
                }
                auth.addAuthStateListener(listener)
                trySend(auth.currentUser?.uid)
                awaitClose { auth.removeAuthStateListener(listener) }
            }.distinctUntilChanged().collectLatest { userUid ->
                if (userUid == null) {
                    _notes.value = emptyList()
                } else {
                    observeTrash(userUid).collect { values ->
                        _notes.value = mapper.fromDomain(values)
                    }
                }
            }
        }
    }

    fun restore(note: Note) {
        screenModelScope.launch {
            restoreNote(note.data.uid, note.data.userUid)
        }
    }

    fun deletePermanently(note: Note) {
        screenModelScope.launch {
            deleteNotePermanently(note.data.uid, note.data.userUid)
        }
    }

    fun empty() {
        val userUid = auth.currentUser?.uid ?: return
        screenModelScope.launch { emptyUserTrash(userUid) }
    }
}
