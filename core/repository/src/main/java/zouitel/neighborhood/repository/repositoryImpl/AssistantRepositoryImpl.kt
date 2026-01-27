package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.repository.AssistantRepo
import zouitel.neighborhood.repository.datasource.AssistantDatasource
import zouitel.neighborhood.repository.mapper.AssistantMapper
import zouitel.neighborhood.domain.model.Assistant as OutAssistant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssistantRepositoryImpl(
    private val datasource: AssistantDatasource,
    private val mapper: AssistantMapper
): AssistantRepo {
    override val observe: Flow<OutAssistant?>
        get() = datasource.observe.map { mapper.toDomain(it) }

    override suspend fun upsert(assistant: OutAssistant) {
        datasource.upsert(mapper.fromDomain(assistant))
    }
}