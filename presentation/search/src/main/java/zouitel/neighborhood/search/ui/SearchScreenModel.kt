package zouitel.neighborhood.search.ui

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.search.mapper.NoteMapper
import zouitel.neighborhood.search.state.UiState
import zouitel.neighborhood.search.utils.AppScreen
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Note
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@Stable
@OptIn(FlowPreview::class)
class SearchScreenModel(
    private val _notes_: NoteUseCase.ObserveAll,  // Keep for fallback
    private val observeDefaultNotes: NoteUseCase.ObserveByDefault,  // 🔥 NEW: User-specific notes
    private val priorities: NoteUseCase.ObserveByPriority,
    private val noteMapper: NoteMapper,
    private val firebaseAuth: FirebaseAuth
): ScreenModel {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.withFlow(UiState())

    // 🔥 Track current user ID
    private val _currentUserId = MutableStateFlow<String?>(firebaseAuth.currentUser?.uid)

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes = _notes
        .debounce(1000)
        .withFlow(emptyList())

    private val _notesByPriority = MutableStateFlow<List<Note>>(emptyList())
    val notesByPriority = _notesByPriority.withFlow(emptyList())

    init {
        // Setup Firebase Auth state listener
        setupAuthStateListener()
    }

    // Observe Firebase Auth state changes
    private fun setupAuthStateListener() {
        screenModelScope.launch {
            callbackFlow {
                val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                    val uid = auth.currentUser?.uid
                    Log.d("SearchScreenModel", "Auth state changed: $uid")
                    trySend(uid)
                }

                firebaseAuth.addAuthStateListener(authStateListener)
                trySend(firebaseAuth.currentUser?.uid)

                awaitClose {
                    Log.d("SearchScreenModel", "Removing auth listener")
                    firebaseAuth.removeAuthStateListener(authStateListener)
                }
            }.collect { uid ->
                _currentUserId.value = uid
                Log.d("SearchScreenModel", "User ID updated: ${uid ?: "null"}")

                // 🔥 Load notes for current user
                loadNotesForUser(uid)
            }
        }
    }

    // 🔥 NEW: Load notes filtered by user
    private fun loadNotesForUser(userUid: String?) {
        screenModelScope.launch {
            if (userUid != null) {
                Log.d("SearchScreenModel", "Loading notes for user: $userUid")

                // Load user-specific notes
                launch {
                    observeDefaultNotes(userUid).collect { domainNotes ->
                        _notes.value = noteMapper.fromDomain(domainNotes)
                        Log.d("SearchScreenModel", "Loaded ${_notes.value.size} notes for user")
                    }
                }

                // Load priority notes
                launch {
                    priorities(userUid).collect { domainNotes ->
                        _notesByPriority.value = noteMapper.fromDomain(domainNotes)
                    }
                }
            } else {
                // No user signed in - clear notes
                Log.d("SearchScreenModel", "No user signed in, clearing notes")
                _notes.value = emptyList()
                _notesByPriority.value = emptyList()
            }
        }
    }

    // 🔥 Keep this for backward compatibility if needed elsewhere
    fun initializeUser(userUid: String) = withAsync {
        Log.d("SearchScreenModel", "Manual initialize for user: $userUid")
        loadNotesForUser(userUid)
    }

    fun updateSearchTitle(title: String) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(text = title)
        }
    }

    fun updateSearchTag(tag: Tag?) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(tag = tag?.label ?: "")
        }
    }

    fun updateSelection(value: Boolean) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isSelection = value)
        }
    }

    fun updateSelectedNotes(note: Data) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.addSelectedNote(note)
        }
    }

    fun clearSelectionNotes() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedNotes = mutableStateListOf())
        }
    }

    fun updateSearchType(type: SearchTypes) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(typeState = type)
        }
    }

    fun updateCurrentScreen(appScreen: AppScreen) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(currentScreen = appScreen)
        }
    }

    override fun onDispose() {
        super.onDispose()
        Log.d("SearchScreenModel", "Disposing, notes count: ${notes.value.size}")
    }
}