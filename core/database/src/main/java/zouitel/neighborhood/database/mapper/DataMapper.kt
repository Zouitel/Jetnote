package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Data
import zouitel.neighborhood.repository.model.Data as OutData

class DataMapper {
    fun fromRepo(data: List<OutData>) = data.map { fromRepo(it) }

    fun toRepo(data: Data) = OutData(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        description = data.description,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = data.date,
        lastEdit = data.lastEdit,
        archived = data.archived,
        blocks = data.blocks,
        tasks = data.tasks,
        medias = data.medias,
        audios = data.audios,
        reminders = data.reminders
    )

    fun fromRepo(data: OutData) = Data(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        description = data.description,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = data.date,
        lastEdit = data.lastEdit,
        archived = data.archived,
        blocks = data.blocks,
        tasks = data.tasks,
        medias = data.medias,
        audios = data.audios,
        reminders = data.reminders
    )
}