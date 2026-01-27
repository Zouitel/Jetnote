package zouitel.neighborhood.workspace.ui.features.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
                    containerColor = CommonColors.azure,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Pick", fontSize = 16.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                {
                    onAction.invoke()
                    timePickerDialog.value = false
                },
                modifier = Modifier.border(1.dp, CommonColors.azure, CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = CommonColors.azure
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
                clockDialSelectedContentColor = Color.White,
                clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                selectorColor = CommonColors.azure,
                timeSelectorSelectedContainerColor = CommonColors.azure,
                timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                timeSelectorSelectedContentColor = Color.White,
                timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}