package zouitel.neighborhood.workspace.ui.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel

fun LazyListScope.tagsContext(
    uid: String,
    filteredTags: List<Tag>,
    noteAndTagModel: NoteAndTagScreenModel,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    deletedTagId: SnapshotStateList<Pair<Long, Int>>,
    jobCounter: MutableIntState
) {
    item {
        val deleteResource = stringResource(R.string.delete)
        val undoResource = stringResource(R.string.action_undo)

        ContextualFlowRow(
            itemCount = filteredTags.size,
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 10.dp)
        ) { index ->
            SuggestionChip(
                modifier = Modifier
                    .alpha(.8f)
                    .padding(1.5.dp),
                onClick = {},
                label = {
                    Text(
                        modifier = Modifier
                            .combinedClickable(onLongClick = {
                                val currentJobId = jobCounter.intValue++
                                deletedTagId.add(
                                    Pair(
                                        filteredTags.getOrNull(index)?.id
                                            ?: return@combinedClickable, currentJobId
                                    )
                                )
                                val tagPair =
                                    deletedTagId.firstOrNull { it.second == currentJobId }

                                noteAndTagModel.sendAction(
                                    Action.DeleteById(
                                        filteredTags.getOrNull(index)?.id
                                            ?: return@combinedClickable
                                    )
                                )

                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "${filteredTags.getOrNull(index)?.label} $deleteResource",
                                        actionLabel = undoResource,
                                        duration = SnackbarDuration.Long
                                    )


                                    if (result == SnackbarResult.ActionPerformed) {
                                        tagPair?.first?.let { tag ->
                                            noteAndTagModel.sendAction(
                                                Action.Insert(NoteAndTag(uid, tag))
                                            )
                                        }
                                    }
                                    tagPair?.let { deletedTagId.remove(it) }
                                }
                            }) {},
                        text = filteredTags.getOrNull(index)?.label ?: ""
                    )
                },
                border = SuggestionChipDefaults.suggestionChipBorder(false),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    Color(
                        filteredTags.getOrNull(index)?.color
                            ?: MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
                    ).copy(alpha = .5f),
                    Color(
                        filteredTags.getOrNull(index)?.color
                            ?: MaterialTheme.colorScheme.onSurface.toArgb()
                    )
                )
            )
        }
    }
}