package zouitel.neighborhood.assistant.ui.ai_model

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.ai.provider.Model as ProviderModel
import zouitel.neighborhood.domain.usecase.ModelUseCase
import zouitel.neighborhood.assistant.mapper.ModelMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
class ModelsScreenModel(
    private val _observeAll_: ModelUseCase.ObserveAll,
    private val _upsert: ModelUseCase.Upsert,
    private val _delete: ModelUseCase.Delete,
    private val mapper: ModelMapper
): ScreenModel {
    private val _observeAll = MutableStateFlow<List<ProviderModel>>(emptyList())
    val observeAll: Flow<List<ProviderModel>> = _observeAll
        .stateIn(
            screenModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

    init {
        screenModelScope.launch {
            _observeAll_().collect {
                _observeAll.value = mapper.fromDomain(it)
            }
        }
    }

    fun upsert(model: ProviderModel) {
        screenModelScope.launch {
            _upsert(mapper.toDomain(model))
        }
    }

    fun delete(id: String) {
        screenModelScope.launch {
            _delete(id)
        }
    }
}