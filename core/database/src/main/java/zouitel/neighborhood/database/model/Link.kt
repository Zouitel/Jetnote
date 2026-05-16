package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import zouitel.neighborhood.database.model.Link.Companion.TABLE_NAME

@Entity(TABLE_NAME, [Index("url")])
data class Link(
    @PrimaryKey
    @ColumnInfo("url", defaultValue = "") val url: String,
    @ColumnInfo("uuid", defaultValue = "") val uid: String,
    @ColumnInfo("title", defaultValue = "") val title: String,
    @ColumnInfo("description", defaultValue = "") val description: String,
    @ColumnInfo("imageg_link", defaultValue = "") val image: String,
    @ColumnInfo("icon", defaultValue = "") val icon: String,
    @ColumnInfo("domain", defaultValue = "") val domain: String
) {
    companion object {
        const val TABLE_NAME = "links_table"
    }
}