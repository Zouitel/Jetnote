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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreen
import zouitel.neighborhood.assistant.ui.provider.ProvidersScreen
import zouitel.neighborhood.logic.R
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
            { Text(stringResource(R.string.new_chat), style = MaterialTheme.typography.bodyLarge) },
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
            { Text(stringResource(R.string.assistant), style = MaterialTheme.typography.bodyLarge) },
            {
                expanded.value = false
                navigator?.push(AssistantScreen(assistant))
            },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.SETTINGS), null)
            }
        )
        DropdownMenuItem(
            { Text(stringResource(R.string.providers), style = MaterialTheme.typography.bodyLarge) },
            { navigator?.push(ProvidersScreen()) },
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.CUBES), null)
            }
        )
        DropdownMenuItem(
            { Text(stringResource(R.string.retitle), style = MaterialTheme.typography.bodyLarge) },
            onRetitle,
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(painterResource(CommonIcons.TEXT_BOX_EDIT), null)
            }
        )
        DropdownMenuItem(
            {
                Text(
                    text = stringResource(R.string.options),
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            onOptions,
            Modifier.padding(horizontal = 10.dp),
            {
                Icon(
                    painterResource(CommonIcons.CLIP),
                    null,
                )
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            {
                Text(
                    text =stringResource(R.string.delete),
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
    }
}