package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import zouitel.neighborhood.database.model.Data.Companion.TABLE_NAME

@Entity(TABLE_NAME, [Index("user_uid")])
data class Data(
    @PrimaryKey(false)
    @ColumnInfo("uuid") val uid: String,
    @ColumnInfo("user_uid") val userUid: String,
    @ColumnInfo("title") val title: String,
    @ColumnInfo("position") val position: Int,
    @ColumnInfo("priority") val priority: Int,
    @ColumnInfo("color") val background: Int?,
    @ColumnInfo("text_color") val textColor: Int?,
    @ColumnInfo("date") val date: String,
    @ColumnInfo("last_edit") val lastEdit: String,
    @ColumnInfo("archived") val archived: Boolean,
    @ColumnInfo("blocks", defaultValue = "[]") val blocks: String,
    @ColumnInfo("medias", defaultValue = "[]") val medias: String,
    @ColumnInfo("audios", defaultValue = "[]") val audios: String,
    @ColumnInfo("reminders", defaultValue = "[]") val reminders: String
) {
    companion object {
        const val TABLE_NAME = "note_data_table"
    }
}
