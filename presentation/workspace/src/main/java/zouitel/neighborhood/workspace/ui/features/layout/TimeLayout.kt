package zouitel.neighborhood.workspace.ui.features.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.Settings
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun TimeLayout(
    settings: Settings,
    selectedTime: MutableLongState,
    timeState:TimePickerState,
    timePickerDialog: MutableState<Boolean>,
    onAction: () -> Unit
) {
    DatePickerDialog(
        onDismissRequest = {
            timePickerDialog.value = false
        },
        confirmButton = {
            Button(
                onClick = {
                    onAction.invoke()
                    timePickerDialog.value = false
                    selectedTime.longValue = timeState.hour.hours.inWholeMilliseconds + timeState.minute.minutes.inWholeMilliseconds
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CommonColors.prime,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(text = "Pick", fontSize = 16.sp)
            }
        },
        dismissButton = {
            Button(
                {
                    onAction.invoke()
                    timePickerDialog.value = false
                },
                border = BorderStroke(1.dp, CommonColors.prime),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CommonColors.prime,
                )
            ) {
                Text(text = "Cancel", fontSize = 16.sp)
            }
        }
    ) {
        TimePicker(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            state = timeState,
            layoutType = TimePickerLayoutType.Vertical,
            colors = TimePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                clockDialColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                clockDialSelectedContentColor = MaterialTheme.colorScheme.surface,
                clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                selectorColor = CommonColors.prime,
                timeSelectorSelectedContainerColor = CommonColors.prime,
                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                timeSelectorSelectedContentColor = MaterialTheme.colorScheme.surface,
                timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}