package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Message
import zouitel.neighborhood.domain.repository.MessageRepository

sealed class MessageUseCase {

    data class ObserveAllMessages(private val repository: MessageRepository): MessageUseCase() {
        operator fun invoke() = repository.observeAll
    }

    data class InsertMessage(private val repository: MessageRepository): MessageUseCase() {
        suspend operator fun invoke(message: Message) = repository.insert(message)
    }
}