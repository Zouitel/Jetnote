package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.SemanticNoteResult

interface SemanticSearchRepository {
    suspend fun isModelInstalled(): Boolean
    suspend fun installModel()
    suspend fun indexNotes(userUid: String): Int
    suspend fun search(
        userUid: String,
        query: String,
        limit: Int = 20,
    ): List<SemanticNoteResult>
}
