package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.AssistantDao
import zouitel.neighborhood.database.mapper.AssistantMapper
import zouitel.neighborhood.repository.datasource.AssistantDatasource
import zouitel.neighborhood.repository.model.Assistant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssistantDatasourceImpl(
    private val dao: AssistantDao,
    private val mapper: AssistantMapper,
): AssistantDatasource {
    override val observe: Flow<Assistant?>
        get() = dao.observe.map { mapper.toRepo(it) }

    override suspend fun upsert(assistant: Assistant) {
        dao.upsert(mapper.fromRepo(assistant))
    }
}