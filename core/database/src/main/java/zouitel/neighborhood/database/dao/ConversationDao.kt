package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import zouitel.neighborhood.database.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @get:Query("SELECT * FROM CONVERSATION_TABLE WHERE TITLE <> ''")
    val observeAll: Flow<List<Conversation>>

    @Query("SELECT * FROM CONVERSATION_TABLE WHERE uid = :uid")
    fun observeById(uid: String): Flow<Conversation>

    @Upsert(Conversation::class)
    suspend fun upsert(conversation: Conversation)

    @Query("DELETE FROM CONVERSATION_TABLE WHERE uid = :uid")
    suspend fun delete(uid: String)
}