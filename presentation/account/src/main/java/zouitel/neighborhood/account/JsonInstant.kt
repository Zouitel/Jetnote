package zouitel.neighborhood.account

import kotlinx.serialization.json.Json

val JsonInstant by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}