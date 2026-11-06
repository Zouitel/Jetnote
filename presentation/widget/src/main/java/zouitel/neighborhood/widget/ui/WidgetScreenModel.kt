package zouitel.neighborhood.widget.ui

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
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.widget.mapper.WidgetMapper
import zouitel.neighborhood.widget.model.WidgetNote

class WidgetScreenModel(
    private val default: NoteUseCase.ObserveByDefault,
    private val mapper: WidgetMapper,
    private val firebaseAuth: FirebaseAuth
): ScreenModel {
    private val _auth = MutableStateFlow(firebaseAuth.currentUser?.uid)
    val auth: StateFlow<String?> = _auth.asStateFlow()
    private val _observeByDefault = MutableStateFlow<List<WidgetNote>>(emptyList())
    val observeByDefault: StateFlow<List<WidgetNote>> = _observeByDefault.asStateFlow()

    init {
        screenModelScope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }
                firebaseAuth.addAuthStateListener(listener)
                trySend(firebaseAuth.currentUser?.uid)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.distinctUntilChanged().collectLatest { uid ->
                _auth.value = uid
                _observeByDefault.value = emptyList()
                if (uid == null) return@collectLatest
                default(uid).collect { notes ->
                    _observeByDefault.value = mapper.fromDomain(notes)
                }
            }
        }
    }
}