package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Conversation
import zouitel.neighborhood.domain.repository.ConversationRepo

sealed class ConversationUseCase {
    data class ObserveAll(private val repo: ConversationRepo): ConversationUseCase() {
        operator fun invoke() = repo.observeAll
    }

    data class ObserveById(private val repo: ConversationRepo): ConversationUseCase() {
       suspend operator fun invoke(id: String) = repo.observeById(id)
    }

    data class Upsert(private val repo: ConversationRepo): ConversationUseCase() {
        suspend operator fun invoke(conversation: Conversation) = repo.upsert(conversation)
    }

    data class Delete(private val repo: ConversationRepo): ConversationUseCase() {
        suspend operator fun invoke(id: String) = repo.delete(id)
    }
}