package zouitel.neighborhood.domain.usecase

import zouitel.neighborhood.domain.model.SavedNoteTemplate
import zouitel.neighborhood.domain.repository.NoteTemplateRepository

class NoteTemplateUseCase(
    private val repository: NoteTemplateRepository
) {
    fun observeForUser(ownerUid: String) = repository.observeForUser(ownerUid)

    suspend fun save(template: SavedNoteTemplate) = repository.save(template)

    suspend fun delete(ownerUid: String, templateId: String) =
        repository.delete(ownerUid, templateId)
}
