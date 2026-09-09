package zouitel.neighborhood.workspace.ui.features

import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ui.CommonConfirm
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonIcons.CLOCK
import zouitel.neighborhood.ui.CommonIcons.FRIDAY
import zouitel.neighborhood.ui.CommonIcons.MONDAY
import zouitel.neighborhood.ui.CommonIcons.SATURDAY
import zouitel.neighborhood.ui.CommonIcons.SUNDAY
import zouitel.neighborhood.ui.CommonIcons.THURSDAY
import zouitel.neighborhood.ui.CommonIcons.TUESDAY
import zouitel.neighborhood.ui.CommonIcons.WEDNESDAY
import zouitel.neighborhood.ui.CommonOptionItem
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.SoundEffect
import zouitel.neighborhood.workspace.mapper.JsonInstant
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import zouitel.neighborhood.workspace.ui.features.layout.DateLayout
import zouitel.neighborhood.workspace.ui.features.layout.TimeLayout
import org.koin.compose.koinInject
import zouitel.neighborhood.logic.R
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import androidx.compose.ui.platform.LocalLocale

@Composable
fun ReminderSheet(
    data: MutableState<Data>,
    onReminderSet: (() -> Unit)? = null
) {
    val settingsModel = koinInject<SettingsScreenModel>()
    val alarmManagerModel = koinInject<AlarmManagerScreenModel>()

    val context = LocalContext.current
    val  local = LocalLocale.current.platformLocale

    /**
     * This id always should be an integer because of *IntentPadding.getBroadcast()*,
     * that support only integer parameter.*/
    val id = Random.nextInt()

    val calendar = Calendar.getInstance()
        .apply {
            set(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    val sound = SoundEffect()

    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= System.currentTimeMillis() - SINGLE_DAY
            }
        }
    )

    val localDT = LocalDateTime.now()
    val timeState = rememberTimePickerState(
        initialHour = localDT.hour,
        initialMinute = localDT.minute
    )

    val selectedDate = remember { mutableLongStateOf(dateState.selectedDateMillis!!) }
    val selectedTime = remember { mutableLongStateOf(20.hours.inWholeMilliseconds) }

    val datePickerDialog = remember { mutableStateOf(false) }
    val timePickerDialog = remember { mutableStateOf(false) }

    val dateTime = selectedTime.longValue + (selectedDate.longValue - 1.hours.inWholeMilliseconds)

    if (datePickerDialog.value) {
        DateLayout(
            selectedDate,
            dateState,
            datePickerDialog
        ) {
            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
        }
    }

    if (timePickerDialog.value) {
        TimeLayout(
            selectedTime,
            timeState,
            timePickerDialog
        ) {
            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
        }
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", local)
    val timeFormat = SimpleDateFormat("hh:mm a", local)

    val dayFormat = SimpleDateFormat("EEEE", local)
    val dayName = dayFormat.format(Date(selectedDate.longValue))

    val currentDay =
        when (dayName) {
            "Monday" -> MONDAY
            "Tuesday" -> TUESDAY
            "Wednesday" -> WEDNESDAY
            "Thursday" -> THURSDAY
            "Friday" -> FRIDAY
            "Saturday" -> SATURDAY
            else -> SUNDAY
        }

    LazyColumn(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        item {
            CommonOptionItem(
                name = dateFormat.format(Date(selectedDate.longValue)),
                icon = currentDay
            ) {
                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                datePickerDialog.value = true
            }
        }
        item {
            ContextualFlowRow(4) { index ->
                ElevatedFilterChip(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    selected = dateFormat.format(Date(selectedDate.longValue)) == dateFormat.format(
                        Date(calendar.timeInMillis + index.days.inWholeMilliseconds)
                    ),
                    onClick = {
                        selectedDate.longValue = calendar.timeInMillis + index.days.inWholeMilliseconds
                    },
                    label = {
                        when (index) {
                            0 -> Text(stringResource(R.string.today))
                            1 -> Text(stringResource(R.string.tomorrow))
                            2 -> Text(dayFormat.format(Date(calendar.timeInMillis.plus(2.days.inWholeMilliseconds))))
                            3 -> Text(dayFormat.format(Date(calendar.timeInMillis.plus(3.days.inWholeMilliseconds))))
                        }
                    })
            }
        }
        item {
            CommonOptionItem(
                name = "at " + timeFormat.format(selectedTime.longValue),
                icon = CLOCK
            ) {
                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                timePickerDialog.value = true
            }
        }
        item {
            ContextualFlowRow(7) { index ->
                val time = when (index) {
                    0 -> 6.hours
                    1 -> 9.hours
                    2 -> 12.hours
                    3 -> 15.hours
                    4 -> 18.hours
                    5 -> 21.hours
                    else -> 0.hours
                }
                ElevatedFilterChip(
                    modifier = Modifier.padding(horizontal = 5.dp),
                    selected = selectedTime.longValue == time.inWholeMilliseconds,
                    onClick = { selectedTime.longValue = time.inWholeMilliseconds },
                    label = { Text(timeFormat.format(Date(time.inWholeMilliseconds))) }
                )
            }
        }
        item {
            Text(
                modifier = Modifier.padding(start = 20.dp, top = 15.dp),
                text = "${stringResource(R.string.according_to_timing)} ${country(context).getOrThrow()}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f),
            )
        }
        item {
            Spacer(modifier = Modifier.height(10.dp))
        }
        item {
            CommonConfirm(
                onConfirm = {
                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                    runCatching {
                        alarmManagerModel.initializeAlarm(
                            data = JsonInstant.encodeToString(data.value),
                            id = id,
                            atTime = dateTime
                        )
                    }.onSuccess {
                        data.value = data.value.copy(
                            reminders = data.value.reminders.plusElement(Reminder(id, atTime = dateTime))
                        )
                        // Call the callback to close the sheet
                        onReminderSet?.invoke()
                    }
                }
            )
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}