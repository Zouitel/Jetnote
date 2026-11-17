package zouitel.neighborhood.database.embedding

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zouitel.neighborhood.database.dao.NoteTemplateDao
import zouitel.neighborhood.database.model.NoteTemplateEntity
import zouitel.neighborhood.domain.model.SavedNoteTemplate
import zouitel.neighborhood.domain.repository.NoteTemplateRepository

class LocalNoteTemplateRepository(
    private val dao: NoteTemplateDao
) : NoteTemplateRepository {
    override fun observeForUser(ownerUid: String): Flow<List<SavedNoteTemplate>> =
        dao.observeForUser(ownerUid).map { templates -> templates.map(NoteTemplateEntity::toDomain) }

    override suspend fun save(template: SavedNoteTemplate) {
        dao.upsert(template.toEntity())
    }

    override suspend fun delete(ownerUid: String, templateId: String) {
        dao.delete(ownerUid, templateId)
    }

    private fun NoteTemplateEntity.toDomain() = SavedNoteTemplate(
        templateId = templateId,
        ownerUid = ownerUid,
        title = title,
        description = description,
        category = category,
        blocksJson = blocksJson,
        isFavorite = isFavorite,
        isCustom = isCustom,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun SavedNoteTemplate.toEntity() = NoteTemplateEntity(
        templateId = templateId,
        ownerUid = ownerUid,
        title = title,
        description = description,
        category = category,
        blocksJson = blocksJson,
        isFavorite = isFavorite,
        isCustom = isCustom,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
