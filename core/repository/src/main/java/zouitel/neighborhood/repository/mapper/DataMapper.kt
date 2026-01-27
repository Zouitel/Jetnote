package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Data
import zouitel.neighborhood.domain.model.Data as OutData

class DataMapper {
    fun fromDomain(data: List<OutData>) = data.map { fromDomain(it) }

    fun toDomain(data: Data) = OutData(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        description = data.description,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = data.date,
        archived = data.archived,
        tasks = data.tasks,
        medias = data.medias,
        audios = data.audios,
        reminders = data.reminders
    )

    fun fromDomain(data: OutData) = Data(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        description = data.description,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = data.date,
        archived = data.archived,
        tasks = data.tasks,
        medias = data.medias,
        audios = data.audios,
        reminders = data.reminders
    )
}