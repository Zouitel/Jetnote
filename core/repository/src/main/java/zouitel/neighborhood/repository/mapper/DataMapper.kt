package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Data as RepoData
import zouitel.neighborhood.domain.model.Data as DomainData

class DataMapper {
    fun toDomain(data: RepoData) = DomainData(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        position = data.position,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = data.date,
        lastEdit = data.lastEdit,
        archived = data.archived,
        blocks = data.blocks,
        medias = data.medias,
        audios = data.audios,
        reminders = data.reminders,
        deletedAt = data.deletedAt
    )

    fun fromDomain(data: DomainData) = RepoData(
        uid = data.uid,
        userUid = data.userUid,
        title = data.title,
        position = data.position,
        priority = data.priority,
        background = data.background,
        textColor = data.textColor,
        date = data.date,
        lastEdit = data.lastEdit,
        archived = data.archived,
        blocks = data.blocks,
        medias = data.medias,
        audios = data.audios,
        reminders = data.reminders,
        deletedAt = data.deletedAt
    )
}
