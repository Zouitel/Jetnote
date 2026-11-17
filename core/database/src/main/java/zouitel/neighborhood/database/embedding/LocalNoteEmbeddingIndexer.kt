package zouitel.neighborhood.database.embedding

import java.security.MessageDigest
import zouitel.neighborhood.ai.embedding.TextEmbeddingProvider
import zouitel.neighborhood.database.dao.DataDao
import zouitel.neighborhood.database.dao.NoteEmbeddingDao
import zouitel.neighborhood.database.model.EmbeddingVectorCodec
import zouitel.neighborhood.database.model.NoteEmbedding

/**
 * Builds or refreshes the local vector index for one signed-in account.
 *
 * Content fingerprints avoid regenerating vectors for unchanged notes. The
 * provider is expected to be an on-device implementation; note text is passed
 * only to that provider and is not sent to a remote service by this class.
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

        val existing = embeddings.getActiveForUser(userUid, model.id)
            .associateBy { it.noteUid }
        val pending = activeNotes.map { note ->
            val text = NoteEmbeddingText.from(note.title, note.blocks)
            Triple(note, text, fingerprint(text))
        }.filter { (note, _, hash) ->
            existing[note.uid]?.contentHash != hash
        }

        if (pending.isEmpty()) return 0
        val vectors = provider.embedAll(pending.map { it.second })
        check(vectors.size == pending.size) {
            "Embedding provider returned a different number of vectors than inputs."
        }

        val records = pending.zip(vectors).map { (item, vector) ->
            val (note, _, hash) = item
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
                contentHash = hash,
                vector = EmbeddingVectorCodec.encode(vector.toFloatArray()),
                updatedAt = System.currentTimeMillis(),
            )
        }
        embeddings.upsertAll(records)
        return records.size
    }

    private fun fingerprint(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
}
