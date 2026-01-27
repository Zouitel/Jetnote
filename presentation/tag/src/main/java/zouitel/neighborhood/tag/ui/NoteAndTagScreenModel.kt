package zouitel.neighborhood.tag.ui

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.domain.usecase.NoteAndTagUseCase
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.tag.mapper.NoteAndTagMapper
import zouitel.neighborhood.tag.model.NoteAndTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
@Suppress("DeferredResultUnused")
class NoteAndTagScreenModel(
    private val _observeAll_: NoteAndTagUseCase.ObserveAll,
    private val insert: NoteAndTagUseCase.Insert,
    private val delete: NoteAndTagUseCase.Delete,
    private val mapper: NoteAndTagMapper
): ScreenModel {

    private val _observeAll = MutableStateFlow<List<NoteAndTag>>(emptyList())
    val observeAll: StateFlow<List<NoteAndTag>>
        get() = _observeAll
            .withFlow(emptyList()) {
                withAsync {
                    _observeAll_().collect { _observeAll.value = mapper.fromDomain(it) }
                }
            }

    fun sendAction(act: Action) {
        when (act) {
            is Action.Insert<*> -> withAsync { insert(mapper.toDomain(act.data as NoteAndTag)) }
            is Action.DeleteById -> withAsync { delete(act.id as Long) }
            else -> throw Exception("Not implemented")
        }
    }
}