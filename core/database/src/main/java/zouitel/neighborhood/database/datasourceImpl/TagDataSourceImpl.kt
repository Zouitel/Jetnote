package zouitel.neighborhood.database.datasourceImpl

import zouitel.neighborhood.database.dao.TagDao
import zouitel.neighborhood.database.mapper.TagMapper
import zouitel.neighborhood.repository.datasource.TagDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.repository.model.Tag as OutTag

class TagDataSourceImpl(
    private val dao: TagDao,
    private val mapper: TagMapper
): TagDataSource {

    override val observeAll: Flow<List<OutTag>>
        get() = dao.observeAll.map { mapper.toRepo(it) }
    override val getAll: List<OutTag>
        get() = dao.getAll.map { mapper.toRepo(it) }

    override suspend fun insert(tag: OutTag) {
        dao.insert(mapper.fromRepo(tag))
    }

    override suspend fun insertAll(tags: List<OutTag>) {
        dao.insertAll(mapper.fromRepo(tags))
    }

    override suspend fun delete(id: Long) {
        dao.delete(id)
    }
}