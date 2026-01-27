package zouitel.neighborhood.database.mapper

import zouitel.neighborhood.database.model.Link
import zouitel.neighborhood.repository.model.Link as OutLink

class LinkMapper {
    fun toRepo(links: List<Link>) = links.map { toRepo(it) }

    fun fromRepo(links: List<OutLink>) = links.map { fromRepo(it) }

    fun toRepo(link: Link) = OutLink(
        url = link.url,
        uid = link.uid,
        title = link.title,
        description = link.description,
        image = link.image,
        icon = link.icon,
        domain = link.domain
    )

    fun fromRepo(link: OutLink) = Link(
        url = link.url,
        uid = link.uid,
        title = link.title,
        description = link.description,
        image = link.image,
        icon = link.icon,
        domain = link.domain
    )
}