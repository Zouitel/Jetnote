package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Model
import zouitel.neighborhood.domain.repository.ModelRepo

sealed class ModelUseCase {
    data class ObserveAll(val repo: ModelRepo): ModelUseCase() {
        operator fun invoke() = repo.observeAll
    }

    data class ObserveById(val repo: ModelRepo): ModelUseCase() {
        suspend operator fun invoke(id: String) = repo.observeById(id)
    }

    data class ObserveByProviderId(val repo: ModelRepo): ModelUseCase() {
        suspend operator fun invoke(providerId: Short) = repo.observeByProviderId(providerId)
    }

    data class Upsert(val repo: ModelRepo): ModelUseCase() {
        suspend operator fun invoke(model: Model) = repo.upsert(model)
    }

    data class Delete(val repo: ModelRepo): ModelUseCase() {
        suspend operator fun invoke(id: String) = repo.delete(id)
    }
}