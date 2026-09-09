package zouitel.neighborhood.navigator.main_screen

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.usecase.DataUseCase
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.navigator.mapper.NoteMapper
import zouitel.neighborhood.workspace.model.Note

@Stable
@Suppress("DeferredResultUnused")
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
    val auth: StateFlow<String?> = _auth

    // ── Backing mutable snapshot lists (used by the reorderable library) ──
    val defaultNotes = mutableStateListOf<Note>()
    val newestNotes = mutableStateListOf<Note>()
    val oldestNotes = mutableStateListOf<Note>()
    val nameNotes = mutableStateListOf<Note>()
    val priorityNotes = mutableStateListOf<Note>()

    // Flag that blocks the Room flow from overwriting the list during a drag
    private var isDraggingActive = false

    // ── Flow collectors — guard all five with the flag ──────────────────
    private val _observeDefaults = MutableStateFlow<List<Note>>(emptyList())
    val observeDefaults: StateFlow<List<Note>> = _observeDefaults
        .withFlow(emptyList()) {
            withAsync {
                defaults(auth.value ?: "null").collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeDefaults.value = mapped
                    if (!isDraggingActive) {
                        defaultNotes.apply { clear(); addAll(mapped) }
                    }
                }
            }
        }

    // (repeat the same pattern for newest / oldest / names / priorities)
    private val _observeNewest = MutableStateFlow<List<Note>>(emptyList())
    val observeNewest: StateFlow<List<Note>> = _observeNewest
        .withFlow(emptyList()) {
            withAsync {
                newest(auth.value ?: "null").collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeNewest.value = mapped
                    newestNotes.apply { clear(); addAll(mapped) }
                }
            }
        }

    private val _observeOldest = MutableStateFlow<List<Note>>(emptyList())
    val observeOldest: StateFlow<List<Note>> = _observeOldest
        .withFlow(emptyList()) {
            withAsync {
                oldest(auth.value ?: "null").collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeOldest.value = mapped
                    oldestNotes.apply { clear(); addAll(mapped) }
                }
            }
        }

    private val _observeNames = MutableStateFlow<List<Note>>(emptyList())
    val observeNames: StateFlow<List<Note>> = _observeNames
        .withFlow(emptyList()) {
            withAsync {
                names(auth.value ?: "null").collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeNames.value = mapped
                    nameNotes.apply { clear(); addAll(mapped) }
                }
            }
        }

    private val _observePriorities = MutableStateFlow<List<Note>>(emptyList())
    val observePriorities: StateFlow<List<Note>> = _observePriorities
        .withFlow(emptyList()) {
            withAsync {
                priorities(auth.value ?: "null").collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observePriorities.value = mapped
                    priorityNotes.apply { clear(); addAll(mapped) }
                }
            }
        }

    init {
        setupAuthStateListener()
    }

    // ── Called every drag frame — memory only, no Room write ────────────
    fun onNoteMoved(list: MutableList<Note>, from: Int, to: Int) {
        isDraggingActive = true
        list.add(to, list.removeAt(from))   // instant visual update
    }

    // ── Called once when finger lifts — persist final order ─────────────
    fun onDragEnd(list: MutableList<Note>) {
        isDraggingActive = false
        screenModelScope.launch {
            val positions = list
                .mapIndexed { index, note -> note.data.uid to index }
                .toMap()
            reorderNotes(positions)
        }
    }

    private fun setupAuthStateListener() {
        screenModelScope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    trySend(auth.currentUser?.uid)
                }
                firebaseAuth.addAuthStateListener(listener)
                trySend(firebaseAuth.currentUser?.uid)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.collect { uid ->
                _auth.value = uid
                Log.d("MainScreenModel", "User ID updated: ${uid ?: "null"}")
                reloadNotes()
            }
        }
    }

    private fun reloadNotes() {
        val userId = auth.value ?: "null"
        screenModelScope.launch {
            launch {
                defaults(userId).collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeDefaults.value = mapped
                    defaultNotes.apply { clear(); addAll(mapped) }
                }
            }
            launch {
                newest(userId).collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeNewest.value = mapped
                    newestNotes.apply { clear(); addAll(mapped) }
                }
            }
            launch {
                oldest(userId).collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeOldest.value = mapped
                    oldestNotes.apply { clear(); addAll(mapped) }
                }
            }
            launch {
                names(userId).collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observeNames.value = mapped
                    nameNotes.apply { clear(); addAll(mapped) }
                }
            }
            launch {
                priorities(userId).collect { list ->
                    val mapped = mapper.fromDomain(list)
                    _observePriorities.value = mapped
                    priorityNotes.apply { clear(); addAll(mapped) }
                }
            }
        }
    }
}