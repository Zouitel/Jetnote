package zouitel.neighborhood.widget.mapper

import zouitel.neighborhood.workspace.mapper.DataMapper
import zouitel.neighborhood.tag.mapper.TagMapper
import zouitel.neighborhood.widget.model.WidgetNote
import zouitel.neighborhood.domain.model.Note as OutNote

class WidgetMapper(
    private val dataMapper: DataMapper,
    private val tagMapper: TagMapper
) {
    fun fromDomain(notes: List<OutNote>) = notes.map { fromDomain(it) }

    private fun fromDomain(note: OutNote): WidgetNote = with(note){
        WidgetNote(
            data = dataMapper.fromDomain(this@with.data),
            tags = tags.map { tagMapper.fromDomain(it) }
        )
    }
}