package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * FTS4 index for note text. The UUID is stored but deliberately not tokenized;
 * title and serialized blocks are indexed for full-text matching.
 *
 * The table is maintained by the 12→13 migration triggers in [Database].
 */
@Fts4(notIndexed = ["uid"])
@Entity(tableName = "NoteFts")
data class NoteFts(
    @ColumnInfo(name = "uid") val uid: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "blocks") val blocks: String
)
