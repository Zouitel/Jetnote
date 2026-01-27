package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.NoteAndTag
import zouitel.neighborhood.domain.model.NoteAndTag as OutNoteAndTag

class NoteAndTagMapper {
    fun toDomain(notesAndTag: List<NoteAndTag>) = notesAndTag.map { toDomain(it) }

    fun fromDomain(notesAndTag: List<OutNoteAndTag>) = notesAndTag.map { fromDomain(it) }

    fun toDomain(noteAndTag: NoteAndTag) = OutNoteAndTag(
        uid = noteAndTag.uid,
        id = noteAndTag.id
    )

    fun fromDomain(noteAndTag: OutNoteAndTag) = NoteAndTag(
        uid = noteAndTag.uid,
        id = noteAndTag.id
    )
}