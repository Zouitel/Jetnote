package zouitel.neighborhood.search.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.logic.sharNote
import zouitel.neighborhood.print.printNote
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonIcons.COPY
import zouitel.neighborhood.ui.CommonIcons.INBOX_IN
import zouitel.neighborhood.ui.CommonIcons.INBOX_OUT
import zouitel.neighborhood.ui.CommonIcons.PRINT
import zouitel.neighborhood.ui.CommonIcons.SHARE
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import kotlin.uuid.Uuid

@Composable
fun SelectionTopBar(
    searchModel: SearchScreenModel,
    dataModel: DataScreenModel,
    noteAndTagModel: NoteAndTagScreenModel,
    tagModel: TagScreenModel,
    onDelete: (List<Data>) -> Unit
) {
    val context = LocalContext.current
    val uiState by remember(searchModel, searchModel::uiState).collectAsState()
    val newUid by lazy { Uuid.random().toString() }
    val observeNoteAndTags = remember(noteAndTagModel, noteAndTagModel::observeAll).collectAsState()
    val observeLabels = remember(tagModel, tagModel::observeAll).collectAsState()

    TopAppBar(
        navigationIcon = {
            if (uiState.selectedNotes.all { it.archived }) {
                IconButton({
                    uiState.selectedNotes.map {
                        dataModel.sendAction(Action.Rollback(it.uid))
                    }
                    searchModel.clearSelectionNotes()
                    searchModel.updateSelection(false)
                }) {
                    Icon(
                        painter = painterResource(INBOX_OUT),
                        contentDescription = "rollback notes"
                    )
                }
            } else {
                IconButton({
                    uiState.selectedNotes.map {
                        dataModel.sendAction(Action.Archive(it.uid))
                    }
                    searchModel.clearSelectionNotes()
                    searchModel.updateSelection(false)
                }) {
                    Icon(
                        painter = painterResource(INBOX_IN),
                        contentDescription = "archive notes"
                    )
                }
            }
        },
        title = {
            Row(Modifier.cloudy(enabled = uiState.selectedNotes.count() != 1)) {
                IconButton(
                    enabled = uiState.selectedNotes.count() == 1,
                    onClick = {
                    sharNote(
                        context,
                        uiState.selectedNotes.single().title,
                        uiState.selectedNotes.single().plainText()
                    ) {
                        searchModel.clearSelectionNotes()
                        searchModel.updateSelection(false)
                    }
                }) {
                    Icon(
                        painter = painterResource(SHARE),
                        contentDescription = "share a note."
                    )
                }

                IconButton(
                    enabled = uiState.selectedNotes.count() == 1,
                    onClick = {
                    doubletNote(
                        context,
                        dataModel,
                        uiState.selectedNotes.single(),
                        newUid
                    ) {
                        // copy each label.
                        observeLabels.value
                            .filter {
                                observeNoteAndTags.value.contains(
                                    NoteAndTag(
                                        uiState.selectedNotes.single().uid,
                                        it.id
                                    )
                                )
                            }
                            .map {
                                noteAndTagModel.sendAction(
                                    Action.Insert(NoteAndTag(newUid, it.id))
                                )
                            }
                    }
                    searchModel.clearSelectionNotes()
                    searchModel.updateSelection(false)
                }) {
                    Icon(
                        painter = painterResource(COPY),
                        contentDescription = "copy a note.",
                    )
                }

                IconButton(
                    enabled = uiState.selectedNotes.count() == 1,
                    onClick = {
                    printNote(
                        context,
                        uiState.selectedNotes.single().title,
                        uiState.selectedNotes.single().plainText()
                    )
                }) {
                    Icon(
                        painter = painterResource(PRINT),
                        contentDescription = "print a note."
                    )
                }
                IconButton(
                    enabled = uiState.selectedNotes.isNotEmpty(),
                    onClick = {
                        onDelete(uiState.selectedNotes.toList())

                        searchModel.clearSelectionNotes()
                        searchModel.updateSelection(false)
                    },
                ) {
                    Icon(
                        painter = painterResource(CommonIcons.TRASH),
                        contentDescription = "delete note",
                        tint = MaterialTheme.colorScheme.errorContainer
                    )
                }
            }
        },
        actions = {
            SelectionsCount(searchModel)
        }
    )
}
private fun Data.plainText(): String = blocks.mapNotNull { block ->
    (block.content as? BlockContent.Text)?.text
}.joinToString("\\n")
