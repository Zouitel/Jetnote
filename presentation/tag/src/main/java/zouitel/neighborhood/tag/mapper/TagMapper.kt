package zouitel.neighborhood.tag.mapper

import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.domain.model.Tag as OutTag

class TagMapper {

    fun fromDomain(tags: List<OutTag>) = tags.map { fromDomain(it) }
    fun toDomain(tags: List<Tag>) = tags.map { toDomain(it) }

    fun toDomain(tag: Tag) = OutTag(
        id = tag.id,
        label = tag.label,
        color = tag.color
    )

    fun fromDomain(tag: OutTag) = Tag(
        id = tag.id,
        label = tag.label,
        color = tag.color
    )
}