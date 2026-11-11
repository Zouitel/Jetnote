package zouitel.neighborhood.workspace.mapper

import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.Reminder
import kotlin.time.Clock
import kotlin.time.Instant
import zouitel.neighborhood.domain.model.Data as OutData

class DataMapper {
    fun toDomain(data: List<Data>) = data.map { toDomain(it) }
    fun fromDomain(data: List<OutData>) = data.map { fromDomain(it) }

    fun toDomain(data: Data): OutData {
        val now = Clock.System.now()
        return OutData(
            uid = data.uid,
            userUid = data.userUid,
            title = data.title,
            position = data.position,
            priority = data.priority,
            background = data.background,
            textColor = data.textColor,
            date = JsonInstant.encodeToString<Instant>(data.date),
            lastEdit = JsonInstant.encodeToString<Instant>(now),
            archived = data.archived,
            blocks = BlocksJson.encodeToString<List<Block>>(data.blocks),
            medias = JsonInstant.encodeToString<List<Media>>(data.medias),
            audios = JsonInstant.encodeToString<List<Audio>>(data.audios),
            reminders = JsonInstant.encodeToString<List<Reminder>>(data.reminders),
            deletedAt = data.deletedAt
        )
    }

    fun fromDomain(data: OutData): Data = Data(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        position = data.position,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = JsonInstant.decodeFromString<Instant>(data.date),
        lastEdit = JsonInstant.decodeFromString<Instant>(data.lastEdit),
        archived = data.archived,
        blocks = BlocksJson.decodeFromString<List<Block>>(data.blocks),
        medias = JsonInstant.decodeFromString<List<Media>>(data.medias),
        audios = JsonInstant.decodeFromString<List<Audio>>(data.audios),
        reminders = JsonInstant.decodeFromString<List<Reminder>>(data.reminders),
        deletedAt = data.deletedAt
    )
}
