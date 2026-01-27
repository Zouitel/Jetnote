package zouitel.neighborhood.assistant.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.assistant.model.AssistantRegex
import zouitel.neighborhood.assistant.model.QuickMessage
import zouitel.neighborhood.ui.CommonDimmedText
import zouitel.neighborhood.ui.CommonIcons

@Composable
internal fun <T>AdditionalLayout(
    title: String,
    description: String?,
    elements: Array<T>,
    onAction: () -> Unit,
    onLongClick: (T) -> Unit,
    onClick: (T) -> Unit
) {
    val isVisible = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(onAction) {
                Icon(painterResource(CommonIcons.PLUS), "add")
            }
        }
        AnimatedVisibility(
            visible = description.isNullOrEmpty().not(),
            modifier = Modifier.clickable {
                isVisible.value = isVisible.value.not()
            }
        ) {
            if(isVisible.value) {
                Text(
                    text = description ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                )
            } else CommonDimmedText(description ?: "")
        }

        ContextualFlowRow(elements.size) { index ->
            AssistChip(
                {},
                {
                    val label = when (val el = elements.getOrNull(index)) {
                        is QuickMessage -> (el as QuickMessage).title
                        is AssistantRegex -> (el as AssistantRegex).name
                        is CustomHeader -> (el as CustomHeader).name
                        is CustomBody -> (el as CustomBody).key
                        else -> "void"
                    }
                    Text(
                        label,
                        Modifier.combinedClickable(
                            onLongClick = { onLongClick(elements.getOrNull(index) ?: return@combinedClickable) }
                        ) { onClick(elements.getOrNull(index) ?: return@combinedClickable) }
                    )
                },
                Modifier.padding(3.dp)
            )
        }
    }
}