package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("conversation_table", indices = [Index("uid")])
data class Conversation(
    @PrimaryKey
    val uid: String,
    @ColumnInfo("title")
    val title: String,
    @ColumnInfo("nodes")
    val nodes: String,
    @ColumnInfo("truncateIndex", defaultValue = "-1")
    val truncateIndex: Int,
    @ColumnInfo("suggestions", defaultValue = "[]")
    val suggestions: String,
    @ColumnInfo("isPinned", defaultValue = "0")
    val isPinned: Boolean
)

