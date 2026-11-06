package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.NoteAndTag
import zouitel.neighborhood.domain.repository.NoteAndTagRepository

sealed class NoteAndTagUseCase {
    data class ObserveAll(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        operator fun invoke(userUid: String) = repo.observeAll(userUid)
    }

    data class GetAll(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(userUid: String) = repo.getAll(userUid)
    }

    data class Insert(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(noteAndTag: NoteAndTag, userUid: String) =
            repo.insert(noteAndTag, userUid)
    }

    data class InsertAll(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(noteAndTags: List<NoteAndTag>, userUid: String) =
            repo.insertAll(noteAndTags, userUid)
    }

    data class Delete(private val repo: NoteAndTagRepository): NoteAndTagUseCase() {
        suspend operator fun invoke(noteAndTag: NoteAndTag, userUid: String) =
            repo.delete(noteAndTag, userUid)
    }
}