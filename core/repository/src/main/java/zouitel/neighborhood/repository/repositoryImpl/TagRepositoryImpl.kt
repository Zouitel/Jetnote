package zouitel.neighborhood.repository.repositoryImpl

import zouitel.neighborhood.domain.repository.TagRepository
import zouitel.neighborhood.repository.datasource.TagDataSource
import zouitel.neighborhood.repository.mapper.TagMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.domain.model.Tag as OutTag

class TagRepositoryImpl(
    private val dataSource: TagDataSource,
    private val mapper: TagMapper
): TagRepository {
    override val observeAll: Flow<List<OutTag>>
        get() = dataSource.observeAll.map { mapper.toDomain(it) }

    override val getAll: List<OutTag>
        get() = dataSource.getAll.map { mapper.toDomain(it) }

    override suspend fun insert(tag: OutTag) {
        dataSource.insert(mapper.fromDomain(tag))
    }

    override suspend fun insertAll(tags: List<OutTag>) {
        dataSource.insertAll(mapper.fromDomain(tags))
    }

    override suspend fun delete(id: Long) {
        dataSource.delete(id)
    }
}