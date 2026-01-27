package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import zouitel.neighborhood.database.model.Link.Companion.TABLE_NAME
import zouitel.neighborhood.database.utils.Constants.DEFAULT_TXT
import zouitel.neighborhood.database.utils.Constants.DESCRIPTION
import zouitel.neighborhood.database.utils.Constants.ICON
import zouitel.neighborhood.database.utils.Constants.IMG_LINK
import zouitel.neighborhood.database.utils.Constants.TITLE
import zouitel.neighborhood.database.utils.Constants.URL
import zouitel.neighborhood.database.utils.Constants.UUID

@Entity(TABLE_NAME, [Index(URL)])
data class Link(
    @PrimaryKey
    @ColumnInfo(URL, defaultValue = DEFAULT_TXT) val url: String,
    @ColumnInfo(UUID, defaultValue = DEFAULT_TXT) val uid: String,
    @ColumnInfo(TITLE, defaultValue = DEFAULT_TXT) val title: String,
    @ColumnInfo(DESCRIPTION, defaultValue = DEFAULT_TXT) val description: String,
    @ColumnInfo(IMG_LINK, defaultValue = DEFAULT_TXT) val image: String,
    @ColumnInfo(ICON, defaultValue = DEFAULT_TXT) val icon: String,
    @ColumnInfo("domain", defaultValue = DEFAULT_TXT) val domain: String
) {
    companion object {
        const val TABLE_NAME = "links_table"
    }
}