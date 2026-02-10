package zouitel.neighborhood.workspace.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.workspace.model.Data

@Composable
internal fun NoteDates(data: Data) {
    val scope = rememberCoroutineScope()

    val dateTooltipState = rememberTooltipState()
    val lastEditTooltipState = rememberTooltipState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp, 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Creation Date
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip(
                    shape = MaterialTheme.shapes.large,
                    containerColor = CommonColors.prime,
                    contentColor = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Created: ${data.date.toRelativeTime()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            state = dateTooltipState
        ) {
            Text(
                text = data.date.toLocalDateTime(TimeZone.currentSystemDefault()).format(formatter),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            dateTooltipState.show()
                        }
                    }
                    .padding(4.dp)
            )
        }

        Text(
            text = "•",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
        )

        // Last Edit
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip(
                    shape = MaterialTheme.shapes.large,
                    containerColor = CommonColors.prime,
                    contentColor = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Last edited: ${data.lastEdit.toRelativeTime()}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            state = lastEditTooltipState
        ) {
            Text(
                text = data.lastEdit.toLocalDateTime(TimeZone.currentSystemDefault()).format(formatter),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            lastEditTooltipState.show()
                        }
                    }
                    .padding(4.dp)
            )
        }
    }
}