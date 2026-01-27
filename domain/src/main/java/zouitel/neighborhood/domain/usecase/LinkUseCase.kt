package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.Link
import zouitel.neighborhood.domain.repository.LinkRepository

sealed class LinkUseCase {
    data class ObserveAll(private val repo: LinkRepository) : LinkUseCase() {
        operator fun invoke() = repo.observeAll
    }

    data class GetAll(private val repo: LinkRepository) : LinkUseCase() {
        operator fun invoke() = repo.getAll
    }

    data class ObserveByUid(private val repo: LinkRepository) : LinkUseCase() {
        suspend operator fun invoke(uid: String) = repo.observeByUid(uid)
    }

    data class Insert(private val repo: LinkRepository): LinkUseCase() {
        suspend operator fun invoke(link: Link) = repo.insert(link)
    }

    data class InsertAll(private val repo: LinkRepository): LinkUseCase() {
        suspend operator fun invoke(links: List<Link>) = repo.insertAll(links)
    }

    data class Delete(private val repo: LinkRepository) : LinkUseCase() {
        suspend operator fun invoke(link: Link) = repo.delete(link)
    }
}