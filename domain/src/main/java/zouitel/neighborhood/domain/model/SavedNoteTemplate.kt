package zouitel.neighborhood.domain.model

data class SavedNoteTemplate(
    val templateId: String,
    val ownerUid: String,
    val title: String,
    val description: String,
    val category: String,
    val blocksJson: String,
    val isFavorite: Boolean,
    val isCustom: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
