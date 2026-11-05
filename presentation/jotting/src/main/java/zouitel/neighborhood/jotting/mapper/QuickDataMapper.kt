package zouitel.neighborhood.jotting.mapper

import zouitel.neighborhood.jotting.model.QuickData
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.mapper.JsonInstant
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.domain.model.Data as OutData

class QuickDataMapper {

    fun toDomain(data: QuickData): OutData {
        val blocks = JsonInstant.encodeToString<List<Block>>(data.blocks)
        val medias = JsonInstant.encodeToString<List<Media>>(data.medias)
        val audios = JsonInstant.encodeToString<List<Audio>>(data.audios)
        val reminders = JsonInstant.encodeToString<List<Reminder>>(data.reminders)
        return OutData(
            uid = data.uid,
            title = data.title,
            position = data.position,
            priority = data.priority,
            background = data.background,
            textColor = data.textColor,
            archived = data.archived,
            blocks = blocks,
            medias = medias,
            userUid = data.userUid,
            date = data.date.toString(),
            lastEdit = data.lastEdit.toString(),
            audios = audios,
            reminders = reminders
        )
    }
}