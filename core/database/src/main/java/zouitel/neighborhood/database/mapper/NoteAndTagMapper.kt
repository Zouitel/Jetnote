package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.NoteAndTag
import zouitel.neighborhood.repository.model.NoteAndTag as OutNoteAndTag

class NoteAndTagMapper {
    fun toRepo(notesAndTag: List<NoteAndTag>) = notesAndTag.map { toRepo(it) }

    fun fromRepo(notesAndTag: List<OutNoteAndTag>) = notesAndTag.map { fromRepo(it) }

    fun toRepo(noteAndTag: NoteAndTag) = OutNoteAndTag(
        uid = noteAndTag.uid,
        id = noteAndTag.id
    )

    fun fromRepo(noteAndTag: OutNoteAndTag) = NoteAndTag(
        uid = noteAndTag.uid,
        id = noteAndTag.id
    )
}