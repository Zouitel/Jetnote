package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Provider
import zouitel.neighborhood.domain.repository.ProviderRepo

sealed class ProviderUseCase {
    data class ObserveAll(private val repo: ProviderRepo): ProviderUseCase() {
        operator fun invoke() = repo.observeAll
    }
    data class ObserveById(private val repo: ProviderRepo): ProviderUseCase() {
        suspend operator fun invoke(id: Long) = repo.observeById(id)
    }
    data class Upsert(private val repo: ProviderRepo): ProviderUseCase() {
        suspend operator fun invoke(provider: Provider) = repo.upsert(provider)
    }
}