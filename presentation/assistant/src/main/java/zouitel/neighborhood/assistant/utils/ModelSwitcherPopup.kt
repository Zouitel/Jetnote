package zouitel.neighborhood.assistant.utils

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons

@Composable
internal fun ModelSwitcherPopup(
    models: List<Model>,
    currentModel: Model?,
    onDismiss: () -> Unit,
    onModelSelect: (Model) -> Unit,
    onAddModel: () -> Unit
) {
    Popup(
        alignment = Alignment.BottomStart,
        offset = IntOffset(x = 0, y = -8),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            tonalElevation = 3.dp,
            modifier = Modifier
                .widthIn(min = 220.dp, max = 280.dp)
                .animateContentSize()
        ) {
            LazyColumn(modifier = Modifier.padding(vertical = 8.dp)) {
                items(models) { m ->
                        val isSelected = m.uid == currentModel?.uid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModelSelect(m) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                                .padding(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Avatar(
                                isLoading = false,
                                logo = m.logoUrl,
                                modelName = m.displayName,
                                onClick = { onModelSelect(m) }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = m.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    painterResource(CommonIcons.CHECK),
                                    contentDescription = "selected",
                                    tint = CommonColors.prime,
                                    modifier = Modifier.animateContentSize()
                                )
                            }
                        }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                // "Add model" row at the bottom
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddModel() }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painterResource(CommonIcons.PLUS),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .padding(8.dp)
                        )
                        Text(
                            text = stringResource(R.string.add_model),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}