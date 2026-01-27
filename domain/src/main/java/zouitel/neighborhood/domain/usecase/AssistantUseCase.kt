package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Assistant
import zouitel.neighborhood.domain.repository.AssistantRepo

sealed class AssistantUseCase {
    data class Observe(private val repo: AssistantRepo) : AssistantUseCase() {
        operator fun invoke() = repo.observe
    }

    data class Upsert(private val repo: AssistantRepo) : AssistantUseCase() {
        suspend operator fun invoke(assistant: Assistant) = repo.upsert(assistant)
    }
}