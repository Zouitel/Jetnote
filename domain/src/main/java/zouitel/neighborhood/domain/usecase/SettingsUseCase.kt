package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Settings
import zouitel.neighborhood.domain.repository.SettingsRepository

sealed class SettingsUseCase {
    class ObserveSettings(private val repo: SettingsRepository): SettingsUseCase() {
        operator fun invoke() = repo.observe
    }
    class SaveSettings(private val repo: SettingsRepository): SettingsUseCase() {
        suspend operator fun invoke(settings: Settings) = repo.update(settings)
    }
}