package zouitel.neighborhood.bin.mapper

import zouitel.neighborhood.bin.model.Audio

class AudioMapper {
    fun fromDomain(audios: List<String>): List<Audio> {
        val res = mutableListOf<List<Audio>>()
        audios.forEach { audio ->
            res.add(JsonInstant.decodeFromString<List<Audio>>(audio))
        }
        return res.flatMap { it }
    }
}