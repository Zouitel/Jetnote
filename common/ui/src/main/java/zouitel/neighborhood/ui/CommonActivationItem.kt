package zouitel.neighborhood.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CommonActivationItem(
    title: String,
    description: String?,
    icon : @Composable (() -> Unit)?,
    checked: Boolean?,
    onChecked: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isVisible = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(.9f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                icon?.invoke()
            }


            AnimatedVisibility(checked != null) {
                Switch(
                    modifier = Modifier.weight(.1f),
                    checked = checked == true,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = CommonColors.azure,
                        checkedThumbColor = Color.White
                    ),
                    onCheckedChange = { onChecked.invoke(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        AnimatedVisibility(
            visible = description.isNullOrEmpty().not(),
            modifier = Modifier.clickable {
                isVisible.value = isVisible.value.not()
            }
        ) {
            if(isVisible.value) {
                SelectionContainer {
                    Text(
                        text = description ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                    )
                }
            } else CommonDimmedText(description ?: "")
        }
    }
}

@Composable
fun CommonActivationItem(
    title: String,
    description: String?,
    icon: @Composable (() -> Unit)?,
    checked: Boolean?,
    onChecked: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    content: @Composable (() -> Unit)
) {
    val isVisible = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clip(MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(.9f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionContainer {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                icon?.invoke()
            }


            AnimatedVisibility(checked != null) {
                Switch(
                    modifier = Modifier.weight(.1f),
                    checked = checked == true,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = CommonColors.azure,
                        checkedThumbColor = Color.White
                    ),
                    onCheckedChange = { onChecked.invoke(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        AnimatedVisibility(
            visible = description.isNullOrEmpty().not(),
            modifier = Modifier.clickable {
                isVisible.value = isVisible.value.not()
            }
        ) {
            if(isVisible.value) {
                SelectionContainer {
                    Text(
                        text = description ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                    )
                }
            } else CommonDimmedText(description ?: "")
        }

        content()
    }
}