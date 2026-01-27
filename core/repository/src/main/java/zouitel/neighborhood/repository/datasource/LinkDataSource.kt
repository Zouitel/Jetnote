package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Link
import kotlinx.coroutines.flow.Flow

interface LinkDataSource {
    val observeAll: Flow<List<Link>>

    val getAll: List<Link>

    suspend fun observeByUid(uid: String): Flow<List<Link>>

    suspend fun insert(link: Link)

    suspend fun insertAll(links: List<Link>)

    suspend fun delete(link: Link)

    suspend fun deleteDrafts()
}