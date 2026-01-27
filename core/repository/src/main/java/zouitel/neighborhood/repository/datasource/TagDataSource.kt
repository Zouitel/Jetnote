package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagDataSource {
    val observeAll: Flow<List<Tag>>

    val getAll: List<Tag>

    suspend fun insert(tag: Tag)

    suspend fun insertAll(tags: List<Tag>)

    suspend fun delete(id: Long)
}