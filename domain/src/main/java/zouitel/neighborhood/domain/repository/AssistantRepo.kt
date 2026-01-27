package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Assistant
import kotlinx.coroutines.flow.Flow

interface AssistantRepo {

    val observe: Flow<Assistant?>

    suspend fun upsert(assistant: Assistant)
}