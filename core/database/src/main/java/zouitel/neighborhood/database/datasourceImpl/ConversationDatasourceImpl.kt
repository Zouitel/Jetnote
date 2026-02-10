package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.ConversationDao
import zouitel.neighborhood.database.mapper.ConversationMapper
import zouitel.neighborhood.repository.datasource.ConversationDatasource
import zouitel.neighborhood.repository.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ConversationDatasourceImpl(
    private val dao: ConversationDao,
    private val mapper: ConversationMapper
): ConversationDatasource {
    override val observeAll: Flow<List<Conversation>>
        get() = dao.observeAll.map { mapper.toRepo(it) }

    override suspend fun observeById(id: String): Flow<Conversation> {
        return dao.observeById(id)
            .distinctUntilChanged()
            .map { mapper.toRepo(it) }
    }

    override suspend fun upsert(conversation: Conversation) {
        dao.upsert(mapper.fromRepo(conversation))
    }

    override suspend fun delete(id: String) {
        dao.delete(id)
    }

    override suspend fun cleanup() {
        dao.cleanup()
    }
}