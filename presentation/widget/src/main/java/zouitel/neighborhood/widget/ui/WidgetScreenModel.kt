package zouitel.neighborhood.widget.ui

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.widget.mapper.WidgetMapper
import zouitel.neighborhood.widget.model.WidgetNote
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class WidgetScreenModel(
    private val default: NoteUseCase.ObserveByDefault,
    private val mapper: WidgetMapper
): ScreenModel {
    private val _auth = MutableStateFlow(FirebaseAuth.getInstance().currentUser?.uid)
    val auth: StateFlow<String?> = _auth

    private val _observeByDefault: MutableStateFlow<List<WidgetNote>> = MutableStateFlow(
        emptyList()
    )
    val observeByDefault: StateFlow<List<WidgetNote>> = _observeByDefault
        .withFlow(emptyList()) {
            withAsync {
                default(auth.value ?: "null").collect {
                    _observeByDefault.value = mapper.fromDomain(it)
                }
            }
        }
        .stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
}