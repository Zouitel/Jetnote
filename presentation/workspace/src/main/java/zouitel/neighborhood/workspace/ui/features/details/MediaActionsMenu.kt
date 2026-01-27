package zouitel.neighborhood.workspace.ui.features.details

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ui.CommonIcons

@Composable
internal fun MediaActionsMenu(
    expanded: MutableState<Boolean>,
    onDuplicate: () -> Unit,
    onPrint: () ->Unit,
    onSend: () -> Unit,
    onDetails: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false },
        shape = MaterialTheme.shapes.extraLarge,
        offset = DpOffset((-20).dp, 0.dp)
    ) {
        DropdownMenuItem(
            { Text("Details", style = MaterialTheme.typography.bodyLarge) },
            {
                onDetails()
                expanded.value = false
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.INFO), null)
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            { Text("Duplicate", style = MaterialTheme.typography.bodyLarge) },
            {
                onDuplicate()
                expanded.value = false
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.COPY), null)
            }
        )
        DropdownMenuItem(
            { Text("Send", style = MaterialTheme.typography.bodyLarge) },
            {
                onSend()
                expanded.value = false
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.PAPER_PLANE_TOP), null)
            }        )
        DropdownMenuItem(
            { Text("Print", style = MaterialTheme.typography.bodyLarge) },
            {
                onPrint()
                expanded.value = false
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.PRINT), null)
            }        )
        HorizontalDivider()
        DropdownMenuItem(
            {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.errorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            {
                onDelete()
                expanded.value = false
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(
                    painterResource(CommonIcons.TRASH),
                    null,
                    tint = MaterialTheme.colorScheme.errorContainer
                )
            }
        )
    }
}