package zouitel.neighborhood.workspace.ui.features.layout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.ui.CommonColors
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun DateLayout(
    selectedDate: MutableState<Long>,
    dateState: DatePickerState,
    dateDialog: MutableState<Boolean>,
    onAction: () -> Unit
) {
    val local = LocalLocale.current.platformLocale
    val formatter = SimpleDateFormat("dd MMM yyyy", local)

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
                    dateDialog.value = false
                },
                border = BorderStroke(1.dp, CommonColors.prime),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = CommonColors.prime
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
                selectedDayContentColor  = MaterialTheme.colorScheme.surface,
                todayDateBorderColor = CommonColors.prime,
                dayInSelectionRangeContainerColor = CommonColors.prime,
                selectedDayContainerColor = CommonColors.prime,
                todayContentColor = CommonColors.prime,
                selectedYearContainerColor = CommonColors.prime,
                currentYearContentColor = CommonColors.prime,
                selectedYearContentColor = MaterialTheme.colorScheme.surface,
                dateTextFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedIndicatorColor = CommonColors.prime,
                    unfocusedIndicatorColor = CommonColors.prime,
                    focusedLabelColor = CommonColors.prime,
                    unfocusedLabelColor = CommonColors.prime
                )
            )
        )
    }
}