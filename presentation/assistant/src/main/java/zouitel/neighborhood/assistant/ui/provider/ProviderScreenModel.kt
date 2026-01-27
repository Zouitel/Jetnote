package zouitel.neighborhood.assistant.ui.provider

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.assistant.mapper.ProviderMapper
import zouitel.neighborhood.domain.usecase.ProviderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
class ProviderScreenModel(
    private val _observeAll_: ProviderUseCase.ObserveAll,
    private val _observeById_: ProviderUseCase.ObserveById,
    private val _upsert: ProviderUseCase.Upsert,
    private val mapper: ProviderMapper
): ScreenModel {
    private val _observeAll = MutableStateFlow<List<ProviderSetting>>(emptyList())
    val observeAll = _observeAll.stateIn(
        screenModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    private val _observeById = MutableStateFlow<ProviderSetting?>(null)
    val observeById = _observeById.stateIn(
        screenModelScope,
        SharingStarted.Lazily,
        null
    )

    init {
        screenModelScope.launch {
            _observeAll_().collect {
                _observeAll.value = mapper.fromDomain(it)
            }
        }
    }

    fun initProviderId(id: Long) {
        screenModelScope.launch {
            _observeById_(id).collect {
                _observeById.value = mapper.fromDomain(it)
            }
        }
    }

    fun upsert(provider: ProviderSetting) {
        screenModelScope.launch {
            when (provider::class) {
                ProviderSetting.Google::class -> _upsert(mapper.toDomain(provider as ProviderSetting.Google))
                ProviderSetting.GlobalAI::class -> _upsert(mapper.toDomain(provider as ProviderSetting.GlobalAI))
                ProviderSetting.Claude::class -> _upsert(mapper.toDomain(provider as ProviderSetting.Claude))
            }
        }
    }
}