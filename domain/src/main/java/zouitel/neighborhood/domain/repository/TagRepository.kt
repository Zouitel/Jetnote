package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    val observeAll: Flow<List<Tag>>

    val getAll: List<Tag>

    suspend fun insert(tag: Tag)

    suspend fun insertAll(tags: List<Tag>)

    suspend fun delete(id: Long)
}