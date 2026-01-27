package zouitel.neighborhood.database.model

import androidx.room.Entity
import zouitel.neighborhood.database.model.NoteAndTag.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME, primaryKeys = ["uid", "id"])
data class NoteAndTag(val uid: String, val id: Long) {
    companion object {
        const val TABLE_NAME = "note_and_label"
    }
}