package zouitel.neighborhood.database.embedding

import zouitel.neighborhood.ai.embedding.TextEmbeddingProvider
import zouitel.neighborhood.database.dao.DataDao
import zouitel.neighborhood.database.dao.NoteEmbeddingDao
import zouitel.neighborhood.database.model.EmbeddingVectorCodec
import zouitel.neighborhood.database.model.NoteEmbedding

/**
 * Builds or refreshes the local vector index for one signed-in account.
 *
 * The provider is injected explicitly and is expected to be an on-device
 * implementation. Note text is passed only to that provider and is never
 * written to a network or a second persistence table by this indexer.
 */
class LocalNoteEmbeddingIndexer(
    private val notes: DataDao,
    private val embeddings: NoteEmbeddingDao,
    private val provider: TextEmbeddingProvider,
) {
    suspend fun indexUser(userUid: String): Int {
        require(userUid.isNotBlank()) { "User UID must not be blank." }

        val model = provider.model
        val activeNotes = notes.getIndexableNotes(userUid)
        embeddings.pruneInactiveForUser(userUid)
        if (activeNotes.isEmpty()) return 0

        val inputs = activeNotes.map { note ->
            buildString {
                append(note.title)
                if (note.blocks.isNotBlank()) {
                    append('\n')
                    append(note.blocks)
                }
            }.trim()
        }
        val vectors = provider.embedAll(inputs)
        check(vectors.size == activeNotes.size) {
            "Embedding provider returned a different number of vectors than inputs."
        }

        val records = activeNotes.zip(vectors).map { (note, vector) ->
            check(vector.modelId == model.id) {
                "Embedding provider returned a vector for an unexpected model."
            }
            check(vector.dimensions == model.dimensions) {
                "Embedding provider returned an unexpected vector dimension."
            }
            NoteEmbedding(
                userUid = userUid,
                noteUid = note.uid,
                modelId = model.id,
                dimensions = vector.dimensions,
                vector = EmbeddingVectorCodec.encode(vector.toFloatArray()),
                updatedAt = System.currentTimeMillis(),
            )
        }
        embeddings.upsertAll(records)
        return records.size
    }
}
