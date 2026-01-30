package zouitel.neighborhood.navigator.settings_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonDimmedText

@Composable
internal fun NavigateItem(
    title: String,
    description: String?,
    icon: @Composable () -> Unit =  {},
    onClicked: () -> Unit = {}
) {
    val isVisible = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .clickable(onClick = onClicked)
            .padding(horizontal = 10.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textDecoration = TextDecoration.Underline
            )
            Spacer(modifier = Modifier.width(10.dp))
            icon()
        }

        Spacer(modifier = Modifier.height(5.dp))

        AnimatedVisibility(
            visible = description.isNullOrEmpty().not(),
            modifier = Modifier.clickable {
                isVisible.value = isVisible.value.not()
            }
        ) {
            if (isVisible.value)
                SelectionContainer {
                    Text(
                        text = description ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                    )
                }
            else CommonDimmedText(description ?: "")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SortesItem(
    title: String,
    description: String?,
    items: List<String>,
    currentItem: String?,
    onItemClicked: (String) -> Unit
) {
    val isVisible = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(.9f),
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        AnimatedVisibility(
            visible = description.isNullOrEmpty().not(),
            modifier = Modifier.clickable {
                isVisible.value = isVisible.value.not()
            }
        ) {
            if (isVisible.value)
                SelectionContainer {
                    Text(
                        text = description!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                    )
                }
            else CommonDimmedText(description!!)
        }

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(Modifier.animateContentSize()) {
            items.fastForEach {
                FilledTonalButton(
                    modifier = Modifier.padding(1.5.dp),
                    onClick = {
                        onItemClicked.invoke(it)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor =
                            if (it == currentItem) CommonColors.prime
                            else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (it == currentItem) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    AnimatedVisibility(visible = it == currentItem) {
                        Icon(painter = painterResource(CommonIcons.CHECK_18), null)

                        Spacer(modifier = Modifier.width(25.dp))
                    }

                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}