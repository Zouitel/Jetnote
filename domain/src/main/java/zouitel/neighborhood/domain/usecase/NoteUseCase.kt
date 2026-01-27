package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.repository.NoteRepository

sealed class NoteUseCase {
    data class ObserveAll(private val repo: NoteRepository): NoteUseCase() {
        operator fun invoke() = repo.observeAll
    }

    data class GetAll(private val repo: NoteRepository): NoteUseCase() {
        operator fun invoke() = repo.getAll
    }

    class ObserveByDefault(private val repo: NoteRepository): NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.observeByDefault(userUid)
    }

    class ObserveByName(private val repos: NoteRepository): NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repos.observeByName(userUid)
    }

    class ObserveByOldest(private val repo: NoteRepository): NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.observeByOldest(userUid)
    }

    class ObserveByNewest(private val repo: NoteRepository): NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.observeByNewest(userUid)
    }

    class ObserveByPriority(private val repo: NoteRepository): NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.observeByPriority(userUid)
    }

    class ObserveArchives(private val repo: NoteRepository): NoteUseCase() {
        suspend operator fun invoke(userUid: String) = repo.observeArchives(userUid)
    }
}
