package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Tag
import zouitel.neighborhood.repository.model.Tag as OutTag

class TagMapper {
    fun toRepo(tags: List<Tag>) = tags.map { toRepo(it) }

    fun fromRepo(tags: List<OutTag>) = tags.map { fromRepo(it) }

    fun toRepo(tag: Tag) = OutTag(
        id = tag.id,
        label = tag.label,
        color = tag.color
    )

    fun fromRepo(tag: OutTag) = Tag(
        id = tag.id,
        label = tag.label,
        color = tag.color
    )
}