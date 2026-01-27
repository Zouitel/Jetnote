package zouitel.neighborhood.workspace.mapper

import kotlinx.serialization.json.Json

val JsonInstant by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}