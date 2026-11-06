package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.NoteAndTag
import zouitel.neighborhood.domain.repository.NoteAndTagRepository

sealed class NoteAndTagUseCase {
    data class ObserveAll(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        operator fun invoke() = repo.observeAll
    }

    data class GetAll(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        operator fun invoke() = repo.getAll
    }

    data class Insert(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(noteAndTag: NoteAndTag) = repo.insert(noteAndTag)
    }

    data class InsertAll(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(noteAndTags: List<NoteAndTag>) = repo.insertAll(noteAndTags)
    }

    data class Delete(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(noteAndTag: NoteAndTag) = repo.delete(noteAndTag)
    }

    data class DeleteDrafts(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke() = repo.deleteDrafts()
    }
}
