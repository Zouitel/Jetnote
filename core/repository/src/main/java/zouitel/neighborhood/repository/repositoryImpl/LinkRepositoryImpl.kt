package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.repository.LinkRepository
import zouitel.neighborhood.repository.datasource.LinkDataSource
import zouitel.neighborhood.repository.mapper.LinkMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.domain.model.Link as OutLink

class LinkRepositoryImpl(
    private val dataSource: LinkDataSource,
    private val mapper: LinkMapper
): LinkRepository {

    override val observeAll: Flow<List<OutLink>>
        get() = dataSource.observeAll.map { mapper.toDomain(it) }

    override val getAll: List<OutLink>
        get() = dataSource.getAll.map { mapper.toDomain(it) }

    override suspend fun observeByUid(uid: String): Flow<List<OutLink>> {
        return dataSource.observeByUid(uid).map { mapper.toDomain(it) }
    }

    override suspend fun insert(link: OutLink) {
        dataSource.insert(mapper.fromDomain(link))
    }

    override suspend fun insertAll(links: List<OutLink>) {
        dataSource.insertAll(mapper.fromDomain(links))
    }

    override suspend fun delete(link: OutLink) {
        dataSource.delete(mapper.fromDomain(link))
    }

    override suspend fun deleteDrafts() {
        dataSource.deleteDrafts()
    }
}