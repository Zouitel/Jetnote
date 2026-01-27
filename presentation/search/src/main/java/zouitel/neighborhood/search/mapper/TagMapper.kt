package zouitel.neighborhood.search.mapper

import zouitel.neighborhood.search.model.Tag
import zouitel.neighborhood.domain.model.Tag as OutTag

class TagMapper {
    fun fromDomain(tag: OutTag) = Tag(
        id = tag.id,
        label = tag.label,
        color = tag.color
    )
}