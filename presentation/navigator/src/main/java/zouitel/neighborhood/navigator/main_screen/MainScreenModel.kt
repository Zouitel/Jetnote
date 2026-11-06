package zouitel.neighborhood.navigator.main_screen

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.navigator.mapper.NoteMapper
import zouitel.neighborhood.workspace.model.Note

@Stable
class MainScreenModel(
    private val defaults: NoteUseCase.ObserveByDefault,
    private val newest: NoteUseCase.ObserveByNewest,
    private val oldest: NoteUseCase.ObserveByOldest,
    private val names: NoteUseCase.ObserveByName,
    private val priorities: NoteUseCase.ObserveByPriority,
    private val reorderNotes: DataUseCase.ReorderNotes,
    private val mapper: NoteMapper,
    private val firebaseAuth: FirebaseAuth
) : ScreenModel {

    private val _auth = MutableStateFlow(firebaseAuth.currentUser?.uid)
    val auth: StateFlow<String?> = _auth.asStateFlow()

    val defaultNotes = mutableStateListOf<Note>()
    val newestNotes = mutableStateListOf<Note>()
    val oldestNotes = mutableStateListOf<Note>()
    val nameNotes = mutableStateListOf<Note>()
    val priorityNotes = mutableStateListOf<Note>()

    private val _observeDefaults = MutableStateFlow<List<Note>>(emptyList())
    val observeDefaults: StateFlow<List<Note>> = _observeDefaults.asStateFlow()

    private val _observeNewest = MutableStateFlow<List<Note>>(emptyList())
    val observeNewest: StateFlow<List<Note>> = _observeNewest.asStateFlow()

    private val _observeOldest = MutableStateFlow<List<Note>>(emptyList())
    val observeOldest: StateFlow<List<Note>> = _observeOldest.asStateFlow()

    private val _observeNames = MutableStateFlow<List<Note>>(emptyList())
    val observeNames: StateFlow<List<Note>> = _observeNames.asStateFlow()

    private val _observePriorities = MutableStateFlow<List<Note>>(emptyList())
    val observePriorities: StateFlow<List<Note>> = _observePriorities.asStateFlow()

    private var isDraggingActive = false

    init {
        observeAuthentication()
    }

    fun onNoteMoved(list: MutableList<Note>, from: Int, to: Int) {
        if (from !in list.indices || to !in list.indices || from == to) return
        isDraggingActive = true
        list.add(to, list.removeAt(from))
    }

    fun onDragEnd(list: List<Note>) {
        if (!isDraggingActive) return
        val positions = list.mapIndexed { index, note -> note.data.uid to index }.toMap()

        screenModelScope.launch {
            try {
                reorderNotes(positions)
            } catch (error: Exception) {
                Log.e("MainScreenModel", "Failed to persist note order", error)
            } finally {
                isDraggingActive = false
                applyLatestSnapshots()
            }
        }
    }

    private fun observeAuthentication() {
        screenModelScope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    trySend(auth.currentUser?.uid)
                }
                firebaseAuth.addAuthStateListener(listener)
                trySend(firebaseAuth.currentUser?.uid)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.distinctUntilChanged().collectLatest { uid ->
                _auth.value = uid
                isDraggingActive = false
                clearNotes()

                if (uid == null) return@collectLatest

                // collectLatest cancels every observer belonging to the previous account.
                coroutineScope {
                    launch {
                        defaults(uid).collect { values ->
                            val mapped = mapper.fromDomain(values)
                            _observeDefaults.value = mapped
                            if (!isDraggingActive) defaultNotes.replaceWith(mapped)
                        }
                    }
                    launch {
                        newest(uid).collect { values ->
                            val mapped = mapper.fromDomain(values)
                            _observeNewest.value = mapped
                            if (!isDraggingActive) newestNotes.replaceWith(mapped)
                        }
                    }
                    launch {
                        oldest(uid).collect { values ->
                            val mapped = mapper.fromDomain(values)
                            _observeOldest.value = mapped
                            if (!isDraggingActive) oldestNotes.replaceWith(mapped)
                        }
                    }
                    launch {
                        names(uid).collect { values ->
                            val mapped = mapper.fromDomain(values)
                            _observeNames.value = mapped
                            if (!isDraggingActive) nameNotes.replaceWith(mapped)
                        }
                    }
                    launch {
                        priorities(uid).collect { values ->
                            val mapped = mapper.fromDomain(values)
                            _observePriorities.value = mapped
                            if (!isDraggingActive) priorityNotes.replaceWith(mapped)
                        }
                    }
                }
            }
        }
    }

    private fun applyLatestSnapshots() {
        defaultNotes.replaceWith(_observeDefaults.value)
        newestNotes.replaceWith(_observeNewest.value)
        oldestNotes.replaceWith(_observeOldest.value)
        nameNotes.replaceWith(_observeNames.value)
        priorityNotes.replaceWith(_observePriorities.value)
    }

    private fun clearNotes() {
        defaultNotes.clear()
        newestNotes.clear()
        oldestNotes.clear()
        nameNotes.clear()
        priorityNotes.clear()
        _observeDefaults.value = emptyList()
        _observeNewest.value = emptyList()
        _observeOldest.value = emptyList()
        _observeNames.value = emptyList()
        _observePriorities.value = emptyList()
    }

    private fun MutableList<Note>.replaceWith(values: List<Note>) {
        clear()
        addAll(values)
    }
}
