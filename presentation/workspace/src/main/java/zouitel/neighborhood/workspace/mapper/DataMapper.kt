package zouitel.neighborhood.workspace.mapper

import android.util.Log
import org.koin.core.logger.Logger
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.model.Task
import kotlin.time.Clock
import kotlin.time.Instant
import zouitel.neighborhood.domain.model.Data as OutData

class DataMapper {

    fun toDomain(data: List<Data>) = data.map { toDomain(it) }

    fun fromDomain(data: List<OutData>) = data.map { fromDomain(it) }

    fun toDomain(data: Data): OutData {
        val now = Clock.System.now()
        val date = JsonInstant.encodeToString<Instant>(data.date)
        val lastEdit = JsonInstant.encodeToString<Instant>(now)
        val blocks = BlocksJson.encodeToString<List<Block>>(data.blocks)
        val tasks = JsonInstant.encodeToString<List<Task>>(data.tasks)
        val medias = JsonInstant.encodeToString<List<Media>>(data.medias)
        val audios = JsonInstant.encodeToString<List<Audio>>(data.audios)
        val reminders = JsonInstant.encodeToString<List<Reminder>>(data.reminders)
        return OutData(
            uid = data.uid,
            userUid = data.userUid,
            title = data.title,
            description = data.description,
            priority = data.priority,
            background = data.background,
            textColor = data.textColor,
            date = date,
            lastEdit = lastEdit,
            archived = data.archived,
            blocks = blocks,
            tasks = tasks,
            medias = medias,
            audios = audios,
            reminders = reminders
        )
    }

    fun fromDomain(data: OutData): Data {
        val date = JsonInstant.decodeFromString<Instant>(data.date)
        val lastEdit = JsonInstant.decodeFromString<Instant>(data.lastEdit)
        val blocks = BlocksJson.decodeFromString<List<Block>>(data.blocks)
        val tasks = JsonInstant.decodeFromString<List<Task>>(data.tasks)
        val medias = JsonInstant.decodeFromString<List<Media>>(data.medias)
        val audios = JsonInstant.decodeFromString<List<Audio>>(data.audios)
        val reminders = JsonInstant.decodeFromString<List<Reminder>>(data.reminders)
        return Data(
            uid = data.uid,
            userUid = data.userUid,
            title = data.title,
            description = data.description,
            priority = data.priority,
            background = data.background,
            textColor = data.textColor,
            date = date,
            lastEdit = lastEdit,
            archived = data.archived,
            blocks = blocks,
            tasks = tasks,
            medias = medias,
            audios = audios,
            reminders = reminders
        )
    }
}