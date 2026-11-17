package zouitel.neighborhood.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import zouitel.neighborhood.database.model.NoteTemplateEntity
import zouitel.neighborhood.database.model.NoteTemplateEntity.Companion.TABLE_NAME

@Dao
interface NoteTemplateDao {
    @Query("""
        SELECT * FROM $TABLE_NAME
        WHERE owner_uid = :ownerUid
        ORDER BY is_favorite DESC, updated_at DESC
    """)
    fun observeForUser(ownerUid: String): Flow<List<NoteTemplateEntity>>

    @Upsert
    suspend fun upsert(template: NoteTemplateEntity)

    @Query("""
        DELETE FROM $TABLE_NAME
        WHERE owner_uid = :ownerUid AND template_id = :templateId
    """)
    suspend fun delete(ownerUid: String, templateId: String)
}
