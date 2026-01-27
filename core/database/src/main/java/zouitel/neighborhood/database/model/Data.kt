package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import zouitel.neighborhood.database.model.Data.Companion.TABLE_NAME
import zouitel.neighborhood.database.utils.Constants.ARCHIVED
import zouitel.neighborhood.database.utils.Constants.COLOR
import zouitel.neighborhood.database.utils.Constants.DATE
import zouitel.neighborhood.database.utils.Constants.DESCRIPTION
import zouitel.neighborhood.database.utils.Constants.PRIORITY
import zouitel.neighborhood.database.utils.Constants.TITLE
import zouitel.neighborhood.database.utils.Constants.TXT_COLOR
import zouitel.neighborhood.database.utils.Constants.USER_UID
import zouitel.neighborhood.database.utils.Constants.UUID

@Entity(TABLE_NAME, [Index(UUID), Index(USER_UID)])
data class Data(
    @PrimaryKey(false)
    @ColumnInfo(UUID) val uid: String,
    @ColumnInfo(USER_UID) val userUid: String,
    @ColumnInfo(TITLE) val title: String,
    @ColumnInfo(DESCRIPTION) val description: String,
    @ColumnInfo(PRIORITY) val priority: Int,
    @ColumnInfo(COLOR) val background: Int?,
    @ColumnInfo(TXT_COLOR) val textColor: Int?,
    @ColumnInfo(DATE) val date: String,
    @ColumnInfo(ARCHIVED) val archived: Boolean,
    @ColumnInfo("tasks", defaultValue = "[]") val tasks: String,
    @ColumnInfo("medias", defaultValue = "[]") val medias: String,
    @ColumnInfo("audios", defaultValue = "[]") val audios: String,
    @ColumnInfo("reminders", defaultValue = "[]") val reminders: String,
) {
    companion object {
        const val TABLE_NAME = "note_data_table"
    }
}
