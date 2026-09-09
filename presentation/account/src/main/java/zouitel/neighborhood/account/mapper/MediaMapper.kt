package zouitel.neighborhood.account.mapper

import zouitel.neighborhood.logic.JsonInstant
import zouitel.neighborhood.account.model.Media

class MediaMapper {
    fun fromDomain(medias: List<String>): List<Media> {
        val res = mutableListOf<List<Media>>()
        medias.forEach { media ->
            res.add(JsonInstant.decodeFromString<List<Media>>(media))
        }
        return res.flatten()
    }
}