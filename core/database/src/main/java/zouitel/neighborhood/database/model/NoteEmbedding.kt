package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

/**
 * A locally generated note vector. Vectors are keyed by account, note and
 * model identity so accounts and embedding-model revisions cannot mix.
 */
@Entity(
    tableName = "note_embedding_table",
    primaryKeys = ["user_uid", "note_uid", "model_id"],
    indices = [
        Index(value = ["user_uid", "model_id"]),
        Index(value = ["note_uid"])
    ]
)
data class NoteEmbedding(
    @ColumnInfo(name = "user_uid") val userUid: String,
    @ColumnInfo(name = "note_uid") val noteUid: String,
    @ColumnInfo(name = "model_id") val modelId: String,
    @ColumnInfo(name = "dimensions") val dimensions: Int,
    @ColumnInfo(name = "content_hash") val contentHash: String,
    @ColumnInfo(name = "vector", typeAffinity = ColumnInfo.BLOB) val vector: ByteArray,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    init {
        require(userUid.isNotBlank()) { "User UID must not be blank." }
        require(noteUid.isNotBlank()) { "Note UID must not be blank." }
        require(modelId.isNotBlank()) { "Embedding model ID must not be blank." }
        require(dimensions > 0) { "Embedding dimensions must be positive." }
        require(contentHash.isNotBlank()) { "Content hash must not be blank." }
        require(vector.size == dimensions * Float.SIZE_BYTES) {
            "Vector byte count must match its dimensions."
        }
    }
}
