package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Note
import zouitel.neighborhood.domain.model.Note as OutNote

class NoteMapper(
    private val dataMapper: DataMapper,
    private val tagMapper: TagMapper
) {
    fun toDomain(notes: List<Note>) = notes.map { toDomain(it) }

    fun toDomain(data: Note): OutNote = with(data){
        OutNote(
            data = dataMapper.toDomain(this.data),
            tags = tags.map { tagMapper.toDomain(it) }
        )
    }
}