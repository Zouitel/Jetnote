package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.LinkDao
import zouitel.neighborhood.database.mapper.LinkMapper
import zouitel.neighborhood.repository.datasource.LinkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.repository.model.Link as OutLink

class LinkDataSourceImpl(
    private val dao: LinkDao,
    private val mapper: LinkMapper
): LinkDataSource {
    override val observeAll: Flow<List<OutLink>>
        get() = dao.observeAll.map { mapper.toRepo(it) }

    override val getAll: List<OutLink>
        get() = dao.getAll.map { mapper.toRepo(it) }

    override suspend fun observeByUid(uid: String): Flow<List<OutLink>> {
        return dao.observeByUid(uid).map { mapper.toRepo(it) }
    }

    override suspend fun insert(link: OutLink) {
        dao.insert(mapper.fromRepo(link))
    }

    override suspend fun insertAll(links: List<OutLink>) {
        dao.insertAll(mapper.fromRepo(links))
    }

    override suspend fun delete(link: OutLink) {
        dao.delete(mapper.fromRepo(link))
    }

    override suspend fun deleteDrafts() {
        dao.deleteDrafts()
    }
}