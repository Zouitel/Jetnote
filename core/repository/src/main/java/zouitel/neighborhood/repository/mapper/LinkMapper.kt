package zouitel.neighborhood.repository.mapper

import zouitel.neighborhood.repository.model.Link
import zouitel.neighborhood.domain.model.Link as OutLink

class LinkMapper {
    fun toDomain(links: List<Link>) = links.map { toDomain(it) }

    fun fromDomain(links: List<OutLink>) = links.map { fromDomain(it) }

    fun toDomain(link: Link) = OutLink(
        url = link.url,
        uid = link.uid,
        title = link.title,
        description = link.description,
        image = link.image,
        icon = link.icon,
        domain = link.domain
    )

    fun fromDomain(link: OutLink) = Link(
        url = link.url,
        uid = link.uid,
        title = link.title,
        description = link.description,
        image = link.image,
        icon = link.icon,
        domain = link.domain
    )
}