package zouitel.neighborhood.tag.ui

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.TagUseCase
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.tag.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import zouitel.neighborhood.tag.model.Tag as InTag

@Stable
@Suppress("DeferredResultUnused")
class TagScreenModel(
    private val tags: TagUseCase.ObserveAll,
    private val insert: TagUseCase.Insert,
    private val delete: TagUseCase.Delete,
    private val mapper: TagMapper
): ScreenModel {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState
            .withFlow(UiState())

    private val _observeAll: MutableStateFlow<List<Tag>> = MutableStateFlow(emptyList())
    val observeAll: StateFlow<List<InTag>>
        get() = _observeAll
            .withFlow(emptyList()) {
                withAsync {
                    tags().collect { tags -> _observeAll.value = mapper.fromDomain(tags) }
                }
            }

    fun sendAction(act: Action) {
        when (act) {
            is Action.Insert<*> -> withAsync { insert(mapper.toDomain(act.data as Tag)) }
            is Action.DeleteById -> withAsync { delete(act.id as Long) }
            else -> throw Exception("Not implemented")
        }
    }

    fun updateId(id: Long = 0L): TagScreenModel {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(currentId = id)
        }
        return this
    }

    fun updateColor(color: Int? = null): TagScreenModel {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(currentColor = color)
        }
        return this
    }

    fun updateLabel(label: String = ""): TagScreenModel {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(currentLabel = label)
        }
        return this
    }
}