package zouitel.neighborhood.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class Note(
    @Embedded val data: Data,
    @Relation(
        parentColumn = "uuid", entityColumn = "id", entity = Tag::class,
        associateBy = Junction(
            NoteAndTag::class,
            parentColumn = "uid",
            entityColumn = "id"
        )
    )
    val tags: List<Tag>
)
