package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import zouitel.neighborhood.database.model.Tag.Companion.TABLE_NAME
import zouitel.neighborhood.database.utils.Constants.COLOR
import zouitel.neighborhood.database.utils.Constants.DEFAULT_NUM
import zouitel.neighborhood.database.utils.Constants.DEFAULT_TXT
import zouitel.neighborhood.database.utils.Constants.ID
import zouitel.neighborhood.database.utils.Constants.LABEL

@Entity(TABLE_NAME, [Index(ID)])
data class Tag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID, defaultValue = DEFAULT_NUM) val id: Long,
    @ColumnInfo(name = LABEL, defaultValue = DEFAULT_TXT) val label: String,
    @ColumnInfo(name = COLOR, defaultValue = "null") val color: Int?
) {
    companion object {
        const val TABLE_NAME = "tags_table"
    }
}