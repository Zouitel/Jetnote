package zouitel.neighborhood.widget.utils

import kotlinx.serialization.json.Json

val JsonInstant by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}