package zouitel.neighborhood.navigator.utils

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
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
import sh.calvin.reorderable.rememberReorderableLazyStaggeredGridState
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.navigator.main_screen.MainScreenModel
import zouitel.neighborhood.search.note_card.NoteCard
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.model.Note

@Composable
fun LazyVerticalGrid(
    model: MainScreenModel,
    notes: MutableList<Note>,
    settingsModel: SettingsScreenModel,
    audioModel: AudioScreenModel,
    linkModel: LinkScreenModel,
    searchModel: SearchScreenModel
) {
    val haptic = LocalHapticFeedback.current

    val lazyStaggeredGridState = rememberLazyStaggeredGridState()
    val reorderableLazyStaggeredGridState =
        rememberReorderableLazyStaggeredGridState(lazyStaggeredGridState) { from, to ->
            model.onNoteMoved(notes, from.index, to.index)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

    LaunchedEffect(reorderableLazyStaggeredGridState.isAnyItemDragging) {
        if (!reorderableLazyStaggeredGridState.isAnyItemDragging) {
            model.onDragEnd(notes)
        }
    }

    LazyVerticalStaggeredGrid(
        state = lazyStaggeredGridState,
        modifier = Modifier.fillMaxSize(),
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(notes, key = { _, item -> item.data.uid }) { index, item ->
            ReorderableItem(
                reorderableLazyStaggeredGridState,
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