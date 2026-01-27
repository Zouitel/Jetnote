package zouitel.neighborhood.bin.mapper

import kotlinx.serialization.json.Json

val JsonInstant by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}