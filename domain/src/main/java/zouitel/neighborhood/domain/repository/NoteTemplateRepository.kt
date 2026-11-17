package zouitel.neighborhood.domain.repository

import kotlinx.coroutines.flow.Flow
import zouitel.neighborhood.domain.model.SavedNoteTemplate

interface NoteTemplateRepository {
    fun observeForUser(ownerUid: String): Flow<List<SavedNoteTemplate>>
    suspend fun save(template: SavedNoteTemplate)
    suspend fun delete(ownerUid: String, templateId: String)
}
