package zouitel.neighborhood.workspace.ui.features.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DateLayout(
    settings: Settings,
    selectedDate: MutableState<Long>,
    dateState: DatePickerState,
    dateDialog: MutableState<Boolean>,
    onAction: () -> Unit
) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    DatePickerDialog(
        onDismissRequest = {
            onAction.invoke()
            dateDialog.value = false
        },
        confirmButton = {
            Button(
                onClick = {
                    onAction.invoke()
                    dateDialog.value = false
                    selectedDate.value = dateState.selectedDateMillis!!
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
                    dateDialog.value = false
                },
                modifier = Modifier.border(1.dp, CommonColors.azure, CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = CommonColors.azure
                )
            ) {
                Text(text = "Cancel", fontSize = 16.sp)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
        ) {
        DatePicker(
            state = dateState,
            title = {
                Text(
                    modifier = Modifier.padding(15.dp),
                    text = "Pick a date",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            headline = {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = runCatching { formatter.format(Date(dateState.selectedDateMillis!!)) }.getOrNull() ?: "..."
                )
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = Color.Gray,
                headlineContentColor = Color.LightGray,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContentColor  = Color.White,
                todayDateBorderColor = CommonColors.azure,
                dayInSelectionRangeContainerColor = CommonColors.azure,
                selectedDayContainerColor = CommonColors.azure,
                todayContentColor = CommonColors.azure,
                selectedYearContainerColor = CommonColors.azure,
                currentYearContentColor = CommonColors.azure,
                selectedYearContentColor = Color.White,
                dateTextFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = CommonColors.azure,
                    unfocusedIndicatorColor = CommonColors.glaucous,
                    focusedLabelColor = CommonColors.azure,
                    unfocusedLabelColor = CommonColors.glaucous
                )
            )
        )
    }
}