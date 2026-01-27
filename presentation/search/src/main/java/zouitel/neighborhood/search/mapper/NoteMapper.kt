package zouitel.neighborhood.search.mapper

import zouitel.neighborhood.workspace.mapper.DataMapper
import zouitel.neighborhood.workspace.model.Note
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.domain.model.Note as OutNote

class NoteMapper(
    private val dataMapper: DataMapper,
    private val tagMapper: TagMapper
) {
    fun fromDomain(notes: List<OutNote>) = notes.map { fromDomain(it) }

    private fun fromDomain(note: OutNote): Note = with(note){
        Note(
            data = dataMapper.fromDomain(this@with.data),
            tags = tags.map { tagMapper.fromDomain(it) }
        )
    }
}