package zouitel.neighborhood.tag.ui

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
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.tag.mapper.NoteAndTagMapper
import zouitel.neighborhood.tag.model.NoteAndTag

@Stable
@Suppress("DeferredResultUnused")
class NoteAndTagScreenModel(
    private val observeAll: NoteAndTagUseCase.ObserveAll,
    private val insert: NoteAndTagUseCase.Insert,
    private val delete: NoteAndTagUseCase.Delete,
    private val mapper: NoteAndTagMapper,
    private val firebaseAuth: FirebaseAuth
): ScreenModel {
    private val _observeAll = MutableStateFlow<List<NoteAndTag>>(emptyList())
    val observeAll: StateFlow<List<NoteAndTag>> = _observeAll.asStateFlow()

    init {
        screenModelScope.launch {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.uid) }
                firebaseAuth.addAuthStateListener(listener)
                trySend(firebaseAuth.currentUser?.uid)
                awaitClose { firebaseAuth.removeAuthStateListener(listener) }
            }.distinctUntilChanged().collectLatest { userUid ->
                _observeAll.value = emptyList()
                if (userUid == null) return@collectLatest
                observeAll(userUid).collect { values ->
                    _observeAll.value = mapper.fromDomain(values)
                }
            }
        }
    }

    fun sendAction(act: Action) {
        val userUid = firebaseAuth.currentUser?.uid?.takeIf(String::isNotBlank) ?: return
        when (act) {
            is Action.Insert<*> -> withAsync { insert(mapper.toDomain(act.data as NoteAndTag), userUid) }
            is Action.Delete<*> -> (act.data as? NoteAndTag)?.let { noteAndTag ->
                withAsync { delete(mapper.toDomain(noteAndTag), userUid) }
            }
            else -> throw Exception("Not implemented")
        }
    }
}