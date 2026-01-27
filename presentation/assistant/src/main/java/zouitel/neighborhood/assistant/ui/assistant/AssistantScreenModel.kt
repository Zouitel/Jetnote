package zouitel.neighborhood.assistant.ui.assistant

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.AssistantUseCase
import zouitel.neighborhood.assistant.mapper.AssistantMapper
import zouitel.neighborhood.assistant.model.Assistant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
class AssistantScreenModel(
    private val _observe_: AssistantUseCase.Observe,
    private val _upsert: AssistantUseCase.Upsert,
    private val mapper: AssistantMapper
): ScreenModel {
     private val _observe = MutableStateFlow<Assistant?>(null)
    val observe = _observe.stateIn(
        screenModelScope,
        SharingStarted.WhileSubscribed(5000),
        Assistant()
    )

    init {
        screenModelScope.launch(Dispatchers.IO) {
            _observe_().collect {
                _observe.value = mapper.fromDomain(it)
            }
        }
    }

    fun upsert(assistant: Assistant) {
        screenModelScope.launch(Dispatchers.IO) {
            _upsert(mapper.toDomain(assistant))
        }
    }
}