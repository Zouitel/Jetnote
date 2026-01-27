package zouitel.neighborhood.ui

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.SettingsUseCase
import zouitel.neighborhood.domain.utils.withAsync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
class SettingsScreenModel(
    private val observeSettings: SettingsUseCase.ObserveSettings,
    private val saveSettings: SettingsUseCase.SaveSettings,
    private val mapper: SettingsMapper
): ScreenModel {

    private val _settings = MutableStateFlow(Settings())
    val observe = _settings.stateIn(
        scope = screenModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )
        init {
            screenModelScope.launch(Dispatchers.IO) {
                observeSettings().collect { _settings.value = mapper.fromDomain(it) }
            }
        }

    fun update(settings: Settings) {
        withAsync {
            saveSettings(mapper.toDomain(settings))
        }
    }
}