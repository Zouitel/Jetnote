package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = NoteTemplateEntity.TABLE_NAME,
    primaryKeys = ["owner_uid", "template_id"],
    indices = [Index("owner_uid"), Index("updated_at")]
)
data class NoteTemplateEntity(
    @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "owner_uid") val ownerUid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "blocks_json") val blocksJson: String,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    companion object {
        const val TABLE_NAME = "note_template_table"
    }
}
