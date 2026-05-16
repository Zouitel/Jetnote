package zouitel.neighborhood.navigator.utils

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.navigator.main_screen.MainScreenModel
import zouitel.neighborhood.search.note_card.NoteCard
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.model.Note

@Composable
fun LazyVerticalList(
    model: MainScreenModel,
    notes: MutableList<Note>,
    settingsModel: SettingsScreenModel,
    audioModel: AudioScreenModel,
    linkModel: LinkScreenModel,
    searchModel: SearchScreenModel
) {
    val haptic = LocalHapticFeedback.current

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            model.onNoteMoved(notes, from.index, to.index)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

    LaunchedEffect(reorderableLazyListState.isAnyItemDragging) {
        if (!reorderableLazyListState.isAnyItemDragging) {
            model.onDragEnd(notes)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(notes, key = { _, item -> item.data.uid }) { index, item ->
            ReorderableItem(
                reorderableLazyListState,
                item.data.uid
            ) { isDragging ->
                val interactionSource = remember { MutableInteractionSource() }

                NoteCard(
                    settingsModel = settingsModel,
                    audioModel = audioModel,
                    linkModel = linkModel,
                    searchModel = searchModel,
                    note = item,
                    isDragging = isDragging,
                    dragModifier = Modifier
                        .draggableHandle()
                        .semantics {
                            customActions = listOf(
                                CustomAccessibilityAction("Move Before") {
                                    if (index > 0) {
                                        model.onNoteMoved(notes, index, index - 1); model.onDragEnd(
                                            notes
                                        ); true
                                    } else false
                                },
                                CustomAccessibilityAction("Move After") {
                                    if (index < notes.size - 1) {
                                        model.onNoteMoved(notes, index, index + 1); model.onDragEnd(
                                            notes
                                        ); true
                                    } else false
                                }
                            )
                        }
                        .longPressDraggableHandle(
                            onDragStarted = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragStopped = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                            },
                            interactionSource = interactionSource,
                        )
                )
            }
        }
    }
}