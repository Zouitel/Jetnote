package zouitel.neighborhood.navigator.tags_screen

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.ui.CommonIcons.CROSS_CIRCLE_18
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.tag.ui.TagScreenModel
import kotlinx.coroutines.launch

@Composable
internal fun HashTagLayout(
    tagModel: TagScreenModel,
    snackbarHostState: SnackbarHostState,
    onClick: (Tag) -> Unit
) {
    val tags by remember(tagModel, tagModel::observeAll).collectAsState()

    val deletedTags = remember { mutableStateListOf<Pair<Tag, Int>>() }
    var jobCounter by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    FlowRow(Modifier.padding(horizontal = 8.dp)){
        tags.fastForEachIndexed { index, _ ->
            ElevatedFilterChip(
                modifier = Modifier.padding(1.5.dp),
                selected = false,
                onClick = { onClick(tags.getOrNull(index) ?: return@ElevatedFilterChip) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            val tagToDelete = tags.getOrNull(index) ?: return@IconButton
                            val currentJobId = jobCounter++
                            deletedTags.add(Pair(tagToDelete, currentJobId))
                            tagModel.sendAction(Action.DeleteById(tagToDelete.id))

                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "${tagToDelete.label} deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )

                                val tagPair = deletedTags.firstOrNull { it.second == currentJobId }
                                if (result == SnackbarResult.ActionPerformed) {
                                    tagPair?.first?.let { tag ->
                                        tagModel.sendAction(Action.Insert(tag))
                                    }
                                }
                                tagPair?.let { deletedTags.remove(it) }
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painterResource(CROSS_CIRCLE_18),
                            null
                        )
                    }
                },
                label = {
                    tags.getOrNull(index)?.label.let {
                        Text(
                            text = it ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                    }
                },
                shape = CircleShape,
                colors = FilterChipDefaults.elevatedFilterChipColors(
                    Color(
                        tags.getOrNull(index)?.color
                            ?: MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
                    ).copy(alpha = .4f),
                    Color(
                        tags.getOrNull(index)?.color
                            ?: MaterialTheme.colorScheme.onSurface.toArgb()
                    ),
                    Color(
                        tags.getOrNull(index)?.color
                            ?: MaterialTheme.colorScheme.onSurface.toArgb()
                    )
                )
            )
        }
    }
}