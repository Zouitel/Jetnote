package zouitel.neighborhood.domain.repository

import zouitel.neighborhood.domain.model.Link
import kotlinx.coroutines.flow.Flow

interface LinkRepository {
    val observeAll: Flow<List<Link>>

    val getAll: List<Link>

    suspend fun observeByUid(uid: String): Flow<List<Link>>

    suspend fun insert(link: Link)

    suspend fun insertAll(links: List<Link>)

    suspend fun delete(link: Link)

    suspend fun deleteDrafts()
}