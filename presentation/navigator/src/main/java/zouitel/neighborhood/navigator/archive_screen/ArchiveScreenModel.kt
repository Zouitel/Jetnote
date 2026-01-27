package zouitel.neighborhood.navigator.archive_screen

import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.domain.usecase.NoteUseCase
import zouitel.neighborhood.domain.utils.withAsync
import zouitel.neighborhood.logic.withFlow
import zouitel.neighborhood.navigator.mapper.NoteMapper
import zouitel.neighborhood.workspace.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Stable
class ArchiveScreenModel(
    private val archives: NoteUseCase.ObserveArchives,
    private val mapper: NoteMapper
): ScreenModel {
    private val _observeArchives = MutableStateFlow<List<Note>>(emptyList())
    val observeArchives: StateFlow<List<Note>> = _observeArchives
        .withFlow(emptyList())

    fun initializeUser(userUid: String) = withAsync {
        archives(userUid).collect { _observeArchives.value = mapper.fromDomain(it) }
    }
}