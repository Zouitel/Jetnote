package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Assistant
import kotlinx.coroutines.flow.Flow

interface AssistantDatasource {

    val observe: Flow<Assistant?>

    suspend fun upsert(assistant: Assistant)
}