package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.repository.SemanticSearchRepository

class SemanticSearchUseCase(
    private val repository: SemanticSearchRepository,
) {
    suspend fun isModelInstalled(): Boolean = repository.isModelInstalled()

    suspend fun installModel() = repository.installModel()

    suspend fun indexNotes(userUid: String): Int = repository.indexNotes(userUid)

    suspend fun search(
        userUid: String,
        query: String,
        limit: Int = 20,
    ) = repository.search(userUid, query, limit)
}
