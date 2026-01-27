package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Tag
import zouitel.neighborhood.domain.repository.TagRepository

sealed class TagUseCase {
    data class ObserveAll(private val repo: TagRepository): TagUseCase() {
        operator fun invoke() = repo.observeAll
    }

    data class GetAll(private val repo: TagRepository): TagUseCase() {
        operator fun invoke() = repo.getAll
    }

    data class Insert(private val repo: TagRepository): TagUseCase() {
        suspend operator fun invoke(tag: Tag) = repo.insert(tag)
    }

    data class InsertAll(private val repo: TagRepository): TagUseCase() {
        suspend operator fun invoke(tags: List<Tag>) = repo.insertAll(tags)
    }

    class Delete(private val repo: TagRepository): TagUseCase() {
        suspend operator fun invoke(id: Long) = repo.delete(id)
    }
}