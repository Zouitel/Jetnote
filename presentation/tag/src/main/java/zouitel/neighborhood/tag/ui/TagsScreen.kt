package zouitel.neighborhood.tag.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons.CROSS_SMALL
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.model.Tag

data class TagsScreen(val uid: String, val background: Int?): Screen {

    @Composable
    override fun Content() {

        CommonBottomSheet(background) {
            Tags(
                tagModel = getScreenModel(),
                noteAndTagModel = getScreenModel()
            )
        }
    }

    @Composable
    private fun Tags(
        tagModel: TagScreenModel,
        noteAndTagModel: NoteAndTagScreenModel
    ) {
        val observeTags by remember(tagModel, tagModel::observeAll).collectAsState()
        val observeNoteAndTag by remember(
            noteAndTagModel,
            noteAndTagModel::observeAll
        ).collectAsState()
        val uiState by remember(tagModel, tagModel::uiState).collectAsState()
        var maxLines by remember { mutableIntStateOf(3) }
        val fieldState = rememberTextFieldState(uiState.currentLabel)
        val focusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        val tagResource = stringResource(zouitel.neighborhood.logic.R.string.tag_placeholder)
        val tagCannotBeEmptyResource = stringResource(zouitel.neighborhood.logic.R.string.tag_cannot_be_empty)
        var placeholder by remember { mutableStateOf("Tag..") }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 25.dp)
        ) {
            item {
                ContextualFlowRow(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.Center,
                    itemCount = observeTags.size,
                    maxLines = maxLines,
                    overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            InputChip(
                                modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 5.dp),
                                selected = true,
                                onClick = { maxLines += 2 },
                                shape = CircleShape,
                                label = {
                                    Text(
                                        text = "+${totalItemCount - shownItemCount}",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CommonColors.prime,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        },
                        collapseIndicator = {
                            if (maxLines > 3) {
                                InputChip(
                                    modifier = Modifier.align(Alignment.CenterVertically).padding(horizontal = 5.dp),
                                    selected = true,
                                    shape = CircleShape,
                                    onClick = { maxLines = 3 },
                                    label = {
                                        Icon(
                                            painter = painterResource(CROSS_SMALL),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                )
                            }
                        }
                    )
                ) { index ->
                    val noteAndTag = NoteAndTag(uid, observeTags.getOrNull(index)?.id ?: 0)

                    InputChip(
                        modifier = Modifier
                            .padding(2.dp)
                            .align(Alignment.CenterVertically),
                        selected = observeNoteAndTag.contains(noteAndTag),
                        onClick = {
                            val act = when {
                                observeNoteAndTag.contains(noteAndTag) -> Action.Delete(noteAndTag)
                                else -> Action.Insert(noteAndTag)
                            }
                            noteAndTagModel.sendAction(act)
                        },
                        label = {
                            Text(
                                text = observeTags[index].label,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = Color(observeTags[index].color ?: MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()).copy(alpha = .5f),
                            selectedLabelColor = Color(observeTags[index].color ?: MaterialTheme.colorScheme.onSurface.toArgb())
                        )
                    )
                }
            }

            item {
                CommonTextField(
                    state = fieldState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .focusRequester(focusRequester),
                    placeholder = placeholder,
                    imeAction = ImeAction.Done,
                    keyboardActions = {
                        val inputTag = fieldState.text.toString().trim()

                        if (inputTag.isEmpty()) {
                            placeholder = tagCannotBeEmptyResource
                            scope.launch {
                                delay(3000)
                                placeholder = tagResource
                            }
                        } else {
                            tagModel.sendAction(
                                Action.Insert(
                                    data = Tag(
                                        id = uiState.currentId,
                                        label = inputTag,
                                        color = uiState.currentColor
                                    )
                                )
                            )
                            tagModel.updateColor().updateId()
                            fieldState.clearText()
                        }
                    }
                )
            }
        }
    }
}