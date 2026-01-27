package zouitel.neighborhood.tag.mapper

import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.domain.model.NoteAndTag as OutNoteAndTag

class NoteAndTagMapper {

    fun fromDomain(notesAndTag: List<OutNoteAndTag>) = notesAndTag.map { fromDomain(it) }
    fun toDomain(notesAndTag: List<NoteAndTag>) = notesAndTag.map { toDomain(it) }

    fun toDomain(noteAndTag: NoteAndTag) = OutNoteAndTag(
        uid = noteAndTag.uid,
        id = noteAndTag.id
    )

    private fun fromDomain(noteAndTag: OutNoteAndTag) = NoteAndTag(
        uid = noteAndTag.uid,
        id = noteAndTag.id
    )
}