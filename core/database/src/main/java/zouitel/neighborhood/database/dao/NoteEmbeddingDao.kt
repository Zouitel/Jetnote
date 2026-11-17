package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.NoteEmbedding
import kotlinx.coroutines.flow.Flow

/**
 * User-scoped access to local note embeddings. Reads join the note table so
 * archived and deleted notes are never returned to semantic search.
 */
@Dao
interface NoteEmbeddingDao {
    @Upsert
    suspend fun upsert(embedding: NoteEmbedding)

    @Upsert
    suspend fun upsertAll(embeddings: List<NoteEmbedding>)

    @Query("""
        SELECT embeddings.* FROM note_embedding_table AS embeddings
        INNER JOIN note_data_table AS notes
            ON notes.uuid = embeddings.note_uid
            AND notes.user_uid = embeddings.user_uid
        WHERE embeddings.user_uid = :userUid
            AND embeddings.model_id = :modelId
            AND notes.archived = 0
            AND notes.deleted_at IS NULL
    """)
    fun observeActiveForUser(
        userUid: String,
        modelId: String
    ): Flow<List<NoteEmbedding>>

    @Query("""
        SELECT embeddings.* FROM note_embedding_table AS embeddings
        INNER JOIN note_data_table AS notes
            ON notes.uuid = embeddings.note_uid
            AND notes.user_uid = embeddings.user_uid
        WHERE embeddings.user_uid = :userUid
            AND embeddings.model_id = :modelId
            AND notes.archived = 0
            AND notes.deleted_at IS NULL
    """)
    suspend fun getActiveForUser(
        userUid: String,
        modelId: String
    ): List<NoteEmbedding>

    @Query("""
        DELETE FROM note_embedding_table
        WHERE user_uid = :userUid AND note_uid = :noteUid
    """)
    suspend fun deleteForNote(userUid: String, noteUid: String)

    @Query("""
        DELETE FROM note_embedding_table
        WHERE user_uid = :userUid AND model_id = :modelId
    """)
    suspend fun deleteForModel(userUid: String, modelId: String)

    @Query("DELETE FROM note_embedding_table WHERE user_uid = :userUid")
    suspend fun deleteForUser(userUid: String)
}
