package zouitel.neighborhood.jotting.mapper

import zouitel.neighborhood.jotting.model.QuickData
import zouitel.neighborhood.workspace.model.Task
import zouitel.neighborhood.workspace.mapper.JsonInstant
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.domain.model.Data as OutData

class QuickDataMapper {

    fun toDomain(data: QuickData): OutData {
        val tasks = JsonInstant.encodeToString<List<Task>>(data.tasks)
        val medias = JsonInstant.encodeToString<List<Media>>(data.medias)
        return OutData(
            uid = data.uid,
            title = data.title,
            description = data.description,
            priority = data.priority,
            background = data.background,
            textColor = data.textColor,
            date = data.date,
            archived = data.archived,
            tasks = tasks,
            medias = medias
        )
    }
}