package zouitel.neighborhood.database.embedding

import zouitel.neighborhood.ai.embedding.EmbeddingGemmaModelInstaller
import zouitel.neighborhood.domain.model.SemanticNoteResult
import zouitel.neighborhood.domain.repository.SemanticSearchRepository

/**
 * Bridges the domain contract to the local Room index and on-device model.
 */
class LocalSemanticSearchRepository(
    private val installer: EmbeddingGemmaModelInstaller,
    private val indexer: LocalNoteEmbeddingIndexer,
    private val semanticSearch: LocalSemanticSearch,
) : SemanticSearchRepository {
    override suspend fun isModelInstalled(): Boolean = installer.isInstalled

    override suspend fun installModel() {
        installer.install()
    }

    override suspend fun indexNotes(userUid: String): Int {
        check(installer.isInstalled) { "Download the local embedding model before indexing notes." }
        return indexer.indexUser(userUid)
    }

    override suspend fun search(
        userUid: String,
        query: String,
        limit: Int,
    ): List<SemanticNoteResult> {
        check(installer.isInstalled) { "Download the local embedding model before semantic search." }
        return semanticSearch.search(userUid, query, limit = limit)
            .map { SemanticNoteResult(noteId = it.noteId, score = it.score) }
    }
}
