package zouitel.neighborhood.navigator.main_screen

import android.util.Log
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.workspace.model.Note
import zouitel.neighborhood.navigator.mapper.NoteMapper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

@Stable
@Suppress("DeferredResultUnused")
class MainScreenModel(
    private val defaults: NoteUseCase.ObserveByDefault,
    private val newest: NoteUseCase.ObserveByNewest,
    private val oldest: NoteUseCase.ObserveByOldest,
    private val names: NoteUseCase.ObserveByName,
    private val priorities: NoteUseCase.ObserveByPriority,
    private val mapper: NoteMapper,
    private val firebaseAuth: FirebaseAuth
): ScreenModel {

    // 🔥 FIX: Observe Firebase Auth state changes
    private val _auth = MutableStateFlow<String?>(firebaseAuth.currentUser?.uid)
    val auth: StateFlow<String?> = _auth

    private val _observeDefaults = MutableStateFlow<List<Note>>(emptyList())
    val observeDefaults: StateFlow<List<Note>> = _observeDefaults
        .withFlow(emptyList()) {
            withAsync {
                defaults(auth.value ?: "null").collect {
                    _observeDefaults.value = mapper.fromDomain(it)
                }
            }
        }

    private val _observeNewest = MutableStateFlow<List<Note>>(emptyList())
    val observeNewest: StateFlow<List<Note>> = _observeNewest
        .withFlow(emptyList()) {
            withAsync {
                newest(auth.value ?: "null").collect {
                    _observeNewest.value = mapper.fromDomain(it)
                }
            }
        }

    private val _observeOldest = MutableStateFlow<List<Note>>(emptyList())
    val observeOldest: StateFlow<List<Note>> = _observeOldest
        .withFlow(emptyList()) {
            withAsync {
                oldest(auth.value ?: "null").collect {
                    _observeOldest.value = mapper.fromDomain(it)
                }
            }
        }

    private val _observeNames = MutableStateFlow<List<Note>>(emptyList())
    val observeNames: StateFlow<List<Note>> = _observeNames
        .withFlow(emptyList()) {
            withAsync {
                names(auth.value ?: "null").collect {
                    _observeNames.value = mapper.fromDomain(it)
                }
            }
        }

    private val _observePriorities = MutableStateFlow<List<Note>>(emptyList())
    val observePriorities: StateFlow<List<Note>> = _observePriorities
        .withFlow(emptyList()) {
            withAsync {
                priorities(auth.value ?: "null").collect {
                    _observePriorities.value = mapper.fromDomain(it)
                }
            }
        }

    init {
        // 🔥 Setup Firebase Auth state listener
        setupAuthStateListener()
    }

    // 🔥 NEW: Observe Firebase Auth state changes
    private fun setupAuthStateListener() {
        screenModelScope.launch {
            callbackFlow {
                val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                    val uid = auth.currentUser?.uid
                    Log.d("MainScreenModel", "Auth state changed: $uid")
                    trySend(uid)
                }

                firebaseAuth.addAuthStateListener(authStateListener)
                trySend(firebaseAuth.currentUser?.uid)

                awaitClose {
                    Log.d("MainScreenModel", "Removing auth listener")
                    firebaseAuth.removeAuthStateListener(authStateListener)
                }
            }.collect { uid ->
                _auth.value = uid
                Log.d("MainScreenModel", "User ID updated: ${uid ?: "null"}")

                // 🔥 Trigger reload of all note flows when user changes
                reloadNotes()
            }
        }
    }

    // 🔥 NEW: Reload all notes when user changes
    private fun reloadNotes() {
        screenModelScope.launch {
            val userId = auth.value ?: "null"
            Log.d("MainScreenModel", "Reloading notes for user: $userId")

            // Force refresh all flows
            launch {
                defaults(userId).collect {
                    _observeDefaults.value = mapper.fromDomain(it)
                }
            }
            launch {
                newest(userId).collect {
                    _observeNewest.value = mapper.fromDomain(it)
                }
            }
            launch {
                oldest(userId).collect {
                    _observeOldest.value = mapper.fromDomain(it)
                }
            }
            launch {
                names(userId).collect {
                    _observeNames.value = mapper.fromDomain(it)
                }
            }
            launch {
                priorities(userId).collect {
                    _observePriorities.value = mapper.fromDomain(it)
                }
            }
        }
    }
}