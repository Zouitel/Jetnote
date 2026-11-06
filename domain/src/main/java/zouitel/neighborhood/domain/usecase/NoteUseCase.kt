package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.repository.NoteRepository

sealed class NoteUseCase {
    data class ObserveAll(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeAll(userUid)
    }

    data class GetAll(private val repo: NoteRepository) : NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.getAll(userUid)
    }

    class ObserveByDefault(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeByDefault(userUid)
    }

    class ObserveByName(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeByName(userUid)
    }

    class ObserveByOldest(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeByOldest(userUid)
    }

    class ObserveByNewest(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeByNewest(userUid)
    }

    class ObserveByPriority(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeByPriority(userUid)
    }

    class ObserveArchives(private val repo: NoteRepository) : NoteUseCase() {
        operator fun invoke(userUid: String) = repo.observeArchives(userUid)
    }

    data class Cleanup(private val repo: NoteRepository) : NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.cleanup(userUid)
    }
}