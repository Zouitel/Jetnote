package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import zouitel.neighborhood.database.model.Tag.Companion.TABLE_NAME

@Entity(TABLE_NAME, [Index("id")])
data class Tag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", defaultValue = "0") val id: Long,
    @ColumnInfo(name = "label", defaultValue = "") val label: String,
    @ColumnInfo(name = "color", defaultValue = "null") val color: Int?
) {
    companion object {
        const val TABLE_NAME = "tags_table"
    }
}