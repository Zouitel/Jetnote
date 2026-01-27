package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Note
import zouitel.neighborhood.repository.model.Note as OutNote

class NoteMapper(
    private val dataMapper: DataMapper,
    private val tagMapper: TagMapper
) {
    fun toRepo(notes: List<Note>) = notes.map { toRepo(it) }

    fun toRepo(data: Note): OutNote = with(data){
        OutNote(
            data = dataMapper.toRepo(this.data),
            tags = tags.map { tagMapper.toRepo(it) }
        )
    }
}