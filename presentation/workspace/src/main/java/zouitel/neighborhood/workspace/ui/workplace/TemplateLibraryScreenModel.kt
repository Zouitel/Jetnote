package zouitel.neighborhood.workspace.ui.workplace

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.model.SavedNoteTemplate
import zouitel.neighborhood.domain.usecase.NoteTemplateUseCase
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.mapper.BlocksJson
import zouitel.neighborhood.workspace.model.Data
import java.util.UUID

class TemplateLibraryScreenModel(
    private val templateUseCase: NoteTemplateUseCase,
    private val firebaseAuth: FirebaseAuth
) : ScreenModel {
    private val _savedTemplates = MutableStateFlow<List<SavedNoteTemplate>>(emptyList())
    val savedTemplates = _savedTemplates.asStateFlow()

    private var observeJob: Job? = null
    private var observedUid: String? = null

    fun observe() {
        val uid = firebaseAuth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            observeJob?.cancel()
            observedUid = null
            _savedTemplates.value = emptyList()
            return
        }
        if (observedUid == uid && observeJob?.isActive == true) return
        observedUid = uid
        observeJob?.cancel()
        observeJob = screenModelScope.launch {
            templateUseCase.observeForUser(uid).collect { templates ->
                if (firebaseAuth.currentUser?.uid == uid) {
                    _savedTemplates.value = templates
                }
            }
        }
    }

    fun setFavorite(
        templateId: String,
        title: String,
        description: String,
        category: String,
        blocks: List<Block>,
        isCustom: Boolean,
        favorite: Boolean
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        screenModelScope.launch {
            val existing = _savedTemplates.value.firstOrNull { it.templateId == templateId }
            if (!favorite && existing?.isCustom != true) {
                templateUseCase.delete(uid, templateId)
                return@launch
            }
            val now = System.currentTimeMillis()
            templateUseCase.save(
                SavedNoteTemplate(
                    templateId = templateId,
                    ownerUid = uid,
                    title = title,
                    description = description,
                    category = category,
                    blocksJson = BlocksJson.encodeToString<List<Block>>(blocks),
                    isFavorite = favorite,
                    isCustom = isCustom,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
    }

    fun saveCustomTemplate(
        note: Data,
        title: String,
        description: String,
        category: String
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val now = System.currentTimeMillis()
        screenModelScope.launch {
            templateUseCase.save(
                SavedNoteTemplate(
                    templateId = UUID.randomUUID().toString(),
                    ownerUid = uid,
                    title = cleanTitle,
                    description = description.trim(),
                    category = category.trim().ifBlank { "Personal" },
                    blocksJson = BlocksJson.encodeToString<List<Block>>(note.blocks),
                    isFavorite = false,
                    isCustom = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    fun deleteTemplate(templateId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        screenModelScope.launch {
            templateUseCase.delete(uid, templateId)
        }
    }
}
