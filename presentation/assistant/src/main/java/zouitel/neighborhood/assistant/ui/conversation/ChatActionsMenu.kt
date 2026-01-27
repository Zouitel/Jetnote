package zouitel.neighborhood.assistant.ui.conversation

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
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.ui.configurations.AssistantConfigurationScreen
import zouitel.neighborhood.ui.CommonIcons

@Composable
internal fun ChatActionsMenu(
    expanded: MutableState<Boolean>,
    assistant: Assistant?,
    onRetitle: () -> Unit,
    onDelete: () -> Unit,
    onOptions: () -> Unit
) {

    val navigator = LocalNavigator.current
    DropdownMenu(
        expanded = expanded.value,
        onDismissRequest = { expanded.value = false },
        shape = MaterialTheme.shapes.extraLarge,
        offset = DpOffset((-20).dp, 0.dp)
    ) {
        DropdownMenuItem(
            { Text("New Chat", style = MaterialTheme.typography.bodyLarge) },
            {
                expanded.value = false
                navigator?.push(ChatScreen(true))
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.COMMENT_MEDICAL), null)
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            { Text("Settings", style = MaterialTheme.typography.bodyLarge) },
            {
                expanded.value = false
                navigator?.push(AssistantConfigurationScreen(assistant))
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.SETTINGS), null)
            }
        )
        DropdownMenuItem(
            { Text("Retitle", style = MaterialTheme.typography.bodyLarge) },
            onRetitle,
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.REFRESH), null)
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.errorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            onDelete,
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(
                    painterResource(CommonIcons.TRASH),
                    null,
                    tint = MaterialTheme.colorScheme.errorContainer
                )
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            {
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            onOptions,
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(
                    painterResource(CommonIcons.PLUS),
                    null,
                )
            }
        )
    }
}