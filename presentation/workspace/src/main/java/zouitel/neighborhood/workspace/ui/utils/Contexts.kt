package zouitel.neighborhood.workspace.ui.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ui.CommonIcons.ALARM_CLOCK
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun LazyListScope.remindersContext(
        alarmModel: AlarmManagerScreenModel,
        snackbarHostState: SnackbarHostState,
        toolTipState: TooltipState,
        currentData: MutableState<Data>,
        scope: CoroutineScope,
        deletedReminder: SnapshotStateList<Pair<Reminder, Int>>,
        jobCounter: MutableIntState
    ) {
        item {
            ContextualFlowRow(
                itemCount = currentData.value.reminders.size,
                modifier = Modifier
                    .animateContentSize()
                    .padding(3.dp)
            ) { index ->
                val reminder =
                    currentData.value.reminders.getOrNull(index) ?: return@ContextualFlowRow
                TooltipBox(
                    TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    {
                        PlainTooltip {
                            Text(
                                formatDateTimePeriod(
                                    currentData.value.reminders.getOrNull(index)?.atTime ?: 0
                                ),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    },
                    toolTipState
                ) {
                    ElevatedAssistChip(
                        modifier = Modifier.padding(start = 8.dp),
                        onClick = {},
                        label = {
                            Text(
                                modifier = Modifier
                                    .animateContentSize()
                                    .combinedClickable(onLongClick = {
                                        val currentJobId = jobCounter.intValue++
                                        deletedReminder.add(Pair(reminder, currentJobId))
                                        val reminderPair = deletedReminder.firstOrNull { rem -> rem.second == currentJobId }
                                        currentData.value = currentData.value.copy(
                                            reminders = currentData.value.reminders.minusElement(
                                                reminder
                                            )
                                        )

                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            val result = snackbarHostState.showSnackbar(
                                                message = "reminder deleted!",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Long
                                            )


                                            if (result == SnackbarResult.ActionPerformed) {
                                                reminderPair?.first?.let { reminder ->
                                                    currentData.value = currentData.value.copy(
                                                        reminders = currentData.value.reminders.plusElement(
                                                            reminder
                                                        )
                                                    )
                                                }
                                            }
                                            reminderPair?.let { rem -> deletedReminder.remove(rem) }
                                            alarmModel.cancelAlarm(reminder.id)
                                        }

                                    }) {
                                        scope.launch { toolTipState.show() }
                                    },
                                text = formatDateTimeRelativeToNow(
                                    currentData.value.reminders.getOrNull(index)?.atTime ?: 0
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (isPassed(currentData.value.reminders[index].atTime)) TextDecoration.LineThrough else TextDecoration.None,
                                color = Color.White
                            )
                        },
                        leadingIcon = {
                            if (currentData.value.reminders.getOrNull(index)?.isPassed == false)
                                Icon(
                                    painterResource(ALARM_CLOCK),
                                    null,
                                    tint = Color.White
                                )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(.6f, .6f, .6f, .5f)
                        ),
                        elevation = AssistChipDefaults.assistChipElevation()
                    )
                }
            }
        }
    }