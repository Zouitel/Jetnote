package zouitel.neighborhood.account.mapper

import zouitel.neighborhood.logic.JsonInstant
import zouitel.neighborhood.account.model.Audio


class AudioMapper {
    fun fromDomain(audios: List<String>): List<Audio> {
        val res = mutableListOf<List<Audio>>()
        audios.forEach { audio ->
            res.add(JsonInstant.decodeFromString<List<Audio>>(audio))
        }
        return res.flatten()
    }
}