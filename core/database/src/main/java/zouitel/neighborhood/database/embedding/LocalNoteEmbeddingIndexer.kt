package zouitel.neighborhood.database.embedding

import java.security.MessageDigest
import kotlin.math.sqrt
import zouitel.neighborhood.ai.embedding.EmbeddingVector
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
        val prepared = activeNotes.map { note ->
            val text = NoteEmbeddingText.from(note.title, note.blocks)
            Triple(note, text, fingerprint(text))
        }
        prepared.filter { (_, text, _) -> text.isBlank() }
            .forEach { (note, _, _) -> embeddings.deleteForNote(userUid, note.uid) }

        val pending = prepared.filter { (note, text, hash) ->
            text.isNotBlank() && existing[note.uid]?.contentHash != hash
        }

        if (pending.isEmpty()) return 0
        val pendingChunks = pending.map { item ->
            item to NoteEmbeddingText.chunks(item.second)
        }
        val allChunks = pendingChunks.flatMap { it.second }
        val vectors = provider.embedAll(allChunks)
        check(vectors.size == allChunks.size) {
            "Embedding provider returned a different number of vectors than inputs."
        }

        var vectorOffset = 0
        val records = pendingChunks.map { (item, chunks) ->
            val (note, _, hash) = item
            val chunkVectors = vectors.subList(vectorOffset, vectorOffset + chunks.size)
            vectorOffset += chunks.size
            chunkVectors.forEach { vector ->
                check(vector.modelId == model.id) {
                    "Embedding provider returned a vector for an unexpected model."
                }
                check(vector.dimensions == model.dimensions) {
                    "Embedding provider returned an unexpected vector dimension."
                }
            }
            val vector = averageVectors(model.id, chunkVectors)
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

    private fun averageVectors(
        modelId: String,
        vectors: List<EmbeddingVector>,
    ): EmbeddingVector {
        require(vectors.isNotEmpty()) { "At least one chunk vector is required." }
        val average = FloatArray(vectors.first().dimensions)
        vectors.forEach { vector ->
            vector.toFloatArray().forEachIndexed { index, value ->
                average[index] += value / vectors.size
            }
        }
        val magnitude = sqrt(average.sumOf { it.toDouble() * it.toDouble() })
        if (magnitude > 0.0) {
            average.indices.forEach { index ->
                average[index] = (average[index] / magnitude).toFloat()
            }
        }
        return EmbeddingVector(modelId, average)
    }

    private fun fingerprint(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
}
