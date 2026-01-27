package zouitel.neighborhood.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import zouitel.neighborhood.database.utils.Constants.ID
import zouitel.neighborhood.database.utils.Constants.UUID

data class Note(
    @Embedded val data: Data,
    @Relation(
        parentColumn = UUID, entityColumn = ID, entity = Tag::class,
        associateBy = Junction(
            NoteAndTag::class,
            parentColumn = "uid",
            entityColumn = "id"
        )
    )
    val tags: List<Tag>
)
