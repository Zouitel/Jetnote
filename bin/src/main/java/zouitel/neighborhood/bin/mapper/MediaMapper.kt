package zouitel.neighborhood.bin.mapper

import zouitel.neighborhood.bin.model.Media

class MediaMapper {
    fun fromDomain(medias: List<String>): List<Media> {
        val res = mutableListOf<List<Media>>()
        medias.forEach { media ->
            res.add(JsonInstant.decodeFromString<List<Media>>(media))
        }
        return res.flatMap { it }
    }
}