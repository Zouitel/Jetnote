package zouitel.neighborhood.repository.datasource

import zouitel.neighborhood.repository.model.Data

interface DataDataSource {

    suspend fun insert(data: Data)

    suspend fun insertAll(data: List<Data>)

    suspend fun combineUser(userUid: String?)

    suspend fun archive(uid: String)

    suspend fun rollback(uid: String)

    suspend fun delete(uid: String)

    suspend fun erase()

    suspend fun sweepDrafts()
}