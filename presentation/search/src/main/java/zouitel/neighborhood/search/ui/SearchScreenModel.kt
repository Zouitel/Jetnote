package zouitel.neighborhood.search.ui

import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.usecase.SemanticSearchUseCase
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.search.mapper.NoteMapper
import zouitel.neighborhood.search.state.UiState
import zouitel.neighborhood.search.utils.AppScreen
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Note
@Stable
class SearchScreenModel(
    private val observeDefaultNotes: NoteUseCase.ObserveByDefault,
    private val priorities: NoteUseCase.ObserveByPriority,
    private val searchNotes: NoteUseCase.Search,
    private val noteMapper: NoteMapper,
    private val firebaseAuth: FirebaseAuth,
    private val semanticSearch: SemanticSearchUseCase
) : ScreenModel {

    // ── Search field ──────────────────────────────────────────────────────────
    //
    // IMPORTANT: this must live here (on the ScreenModel), NOT inside UiState.
    // Reasons:
    //  1. TextFieldState is a Compose mutable-state object; storing it inside a
    //     data class breaks equals/copy and makes StateFlow miss text changes.
    //  2. Replacing the object on every chip tap (new TextFieldState("Links"))
    //     detaches the InputField, resets the IME cursor, and -- in our old
    //     reactive pipeline -- fed the chip label ("Links", "Audios" …) into
    //     the text predicate 280 ms later, breaking all type searches.
    //
    // Call [setFieldText] to programmatically change the displayed text.
    val fieldState = TextFieldState()

    // ── Raw note flows (unfiltered) ───────────────────────────────────────────

    private val _allNotes = MutableStateFlow<List<Note>>(emptyList())

    /** All notes for the current user. Filtering happens in the Composable. */
    val allNotes = _allNotes.asStateFlow()

    private val _notesByPriority = MutableStateFlow<List<Note>>(emptyList())

    /** Notes that have any priority set (priority > 0). */
    val notesByPriority = _notesByPriority.asStateFlow()

    private val _textSearchResults = MutableStateFlow<List<Note>>(emptyList())
    val textSearchResults = _textSearchResults.asStateFlow()

    private var currentUid: String = ""
    private var textSearchJob: Job? = null
    private var semanticSearchJob: Job? = null

    private val _semanticSearchResults = MutableStateFlow<List<String>>(emptyList())
    val semanticSearchResults = _semanticSearchResults.asStateFlow()

    private val _semanticModelInstalled = MutableStateFlow(false)
    val semanticModelInstalled = _semanticModelInstalled.asStateFlow()

    private val _semanticModelInstalling = MutableStateFlow(false)
    val semanticModelInstalling = _semanticModelInstalling.asStateFlow()

    private val _semanticSearchError = MutableStateFlow<String?>(null)
    val semanticSearchError = _semanticSearchError.asStateFlow()

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.withFlow(UiState())

    // ── Auth / data loading ───────────────────────────────────────────────────

    init {
        refreshSemanticModelState()
        setupAuthStateListener()
    }

    private fun refreshSemanticModelState() {
        screenModelScope.launch {
            runCatching { semanticSearch.isModelInstalled() }
                .onSuccess { _semanticModelInstalled.value = it }
                .onFailure { error ->
                    Log.w(TAG, "Unable to inspect local embedding model", error)
                }
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
            }.collectLatest { uid ->
                Log.d(TAG, "Auth → uid=$uid")
                currentUid = uid.orEmpty()
                textSearchJob?.cancel()
                semanticSearchJob?.cancel()
                _textSearchResults.value = emptyList()
                _semanticSearchResults.value = emptyList()
                if (uid.isNullOrBlank()) {
                    _allNotes.value = emptyList()
                    _notesByPriority.value = emptyList()
                } else {
                    updateTextSearch(fieldState.text.toString())
                    // collectLatest cancels both previous-user observers before
                    // subscribing to the newly authenticated user's data.
                    coroutineScope {
                        launch {
                            observeDefaultNotes(uid).collect { domain ->
                                _allNotes.value = noteMapper.fromDomain(domain)
                                Log.d(TAG, "Loaded ${_allNotes.value.size} notes for user: $uid")
                            }
                        }
                        launch {
                            priorities(uid).collect { domain ->
                                _notesByPriority.value = noteMapper.fromDomain(domain)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Field text helpers ────────────────────────────────────────────────────

    /**
     * Programmatically changes the text shown in the search field.
     *
     * Used when a chip is tapped to show a display label ("Links", "Audios" …).
     * Text-mode queries are sent to the indexed search use case. Other search
     * modes continue to use their existing typed filters.
     */
    /** Debounced, indexed full-text search. Results are scoped to the active account. */
    fun updateTextSearch(query: String) {
        textSearchJob?.cancel()
        semanticSearchJob?.cancel()
        if (query.isBlank() || _uiState.value.typeState !is SearchTypes.Text || currentUid.isBlank()) {
            _textSearchResults.value = emptyList()
            _semanticSearchResults.value = emptyList()
            return
        }

        val uid = currentUid
        if (_uiState.value.semanticMode) {
            _textSearchResults.value = emptyList()
            _semanticSearchResults.value = emptyList()
            if (!_semanticModelInstalled.value) return
            semanticSearchJob = screenModelScope.launch {
                delay(300)
                try {
                    semanticSearch.indexNotes(uid)
                    val results = semanticSearch.search(uid, query)
                    if (currentUid == uid && _uiState.value.semanticMode) {
                        _semanticSearchResults.value = results.map { it.noteId }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "Local semantic search failed", error)
                    _semanticSearchError.value = error.message ?: "Semantic search failed."
                }
            }
        } else {
            _semanticSearchResults.value = emptyList()
            textSearchJob = screenModelScope.launch {
                delay(250)
                searchNotes(uid, query).collect { domain ->
                    if (currentUid == uid) _textSearchResults.value = noteMapper.fromDomain(domain)
                }
            }
        }
    }

    fun setSemanticMode(enabled: Boolean) {
        _semanticSearchError.value = null
        _uiState.value = _uiState.value.copy(semanticMode = enabled)
        updateTextSearch(fieldState.text.toString())
    }

    fun installSemanticModel() {
        if (_semanticModelInstalling.value || _semanticModelInstalled.value) return
        screenModelScope.launch {
            _semanticModelInstalling.value = true
            _semanticSearchError.value = null
            try {
                semanticSearch.installModel()
                _semanticModelInstalled.value = semanticSearch.isModelInstalled()
                if (_semanticModelInstalled.value && currentUid.isNotBlank()) {
                    semanticSearch.indexNotes(currentUid)
                    updateTextSearch(fieldState.text.toString())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Unable to install local embedding model", error)
                _semanticSearchError.value = error.message ?: "Unable to download the embedding model."
            } finally {
                _semanticModelInstalling.value = false
            }
        }
    }

    fun setFieldText(text: String) {
        fieldState.edit { replace(0, length, text) }
    }

    /** Clears the field and resets to the default Text search mode. */
    fun clearSearch() {
        setFieldText("")
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(
                typeState = SearchTypes.Text(""),
                tag = "",
            )
        }
    }

    // ── State mutators ────────────────────────────────────────────────────────

    fun updateSearchType(type: SearchTypes) = screenModelScope.launch {
        _uiState.value = _uiState.value.copy(typeState = type)
        if (type is SearchTypes.Text) {
            updateTextSearch(fieldState.text.toString())
        } else {
            textSearchJob?.cancel()
            semanticSearchJob?.cancel()
            _textSearchResults.value = emptyList()
            _semanticSearchResults.value = emptyList()
        }
    }

    fun updateSearchTag(tag: Tag?) = screenModelScope.launch {
        _uiState.value = _uiState.value.copy(tag = tag?.label.orEmpty())
    }

    fun updateSelection(value: Boolean) = screenModelScope.launch {
        _uiState.value = _uiState.value.copy(isSelection = value)
    }

    fun updateSelectedNotes(note: Data) = screenModelScope.launch {
        _uiState.value = _uiState.value.addSelectedNote(note)
    }

    fun clearSelectionNotes() = screenModelScope.launch {
        _uiState.value = _uiState.value.copy(selectedNotes = mutableStateListOf())
    }

    fun updateCurrentScreen(screen: AppScreen) = screenModelScope.launch {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    // ── Back-compat shim ──────────────────────────────────────────────────────
    @Deprecated(
        message = "Pass a plain String.",
        replaceWith = ReplaceWith("setFieldText(fieldState.text.toString())"),
    )
    fun updateSearchTitle(fieldState: TextFieldState) =
        setFieldText(fieldState.text.toString())

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onDispose() {
        super.onDispose()
        textSearchJob?.cancel()
        semanticSearchJob?.cancel()
        Log.d(TAG, "Disposed — ${_allNotes.value.size} notes cached")
    }

    private companion object {
        const val TAG = "SearchScreenModel"
    }
}