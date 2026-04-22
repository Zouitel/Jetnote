package zouitel.neighborhood.assistant.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChooserItem(
    currentAssistant: MutableState<Assistant>,
    title: String,
    description: String?,
    checked: Boolean?,
    onChecked: (Boolean) -> Unit = {},
) {
    val isVisible = remember { mutableStateOf(false) }
    val budgetEntries = mapOf(
        -1 to stringResource(R.string.auto),
        1024 to stringResource(R.string.light),
        16000 to stringResource(R.string.medium),
        32000 to stringResource(R.string.heavy)
    )
    val budgetEntriesList = remember(budgetEntries) { budgetEntries.entries.toList() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp),
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
            AnimatedVisibility(checked != null) {
                Switch(
                    modifier = Modifier.weight(.1f),
                    checked = checked == true,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = CommonColors.prime,
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
            Text(
                text = description!!,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ContextualFlowRow(
            modifier = Modifier.animateContentSize(),
            itemCount = 4
        ) { index ->

            val toolTipState = rememberTooltipState(isPersistent = true)
            val (budgetKey, itemValue) = budgetEntriesList[index]

            TooltipBox(
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(budgetKey.toString(), style = MaterialTheme.typography.titleLarge)
                    }
                },
                state = toolTipState
            ) {
                FilledTonalButton(
                    modifier = Modifier.padding(1.5.dp),
                    onClick = {
                        currentAssistant.value = currentAssistant.value.copy(thinkingBudget = budgetKey)
                    },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor =
                            if (budgetKey == currentAssistant.value.thinkingBudget) CommonColors.prime
                            else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (budgetKey == currentAssistant.value.thinkingBudget) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    AnimatedVisibility(budgetKey == currentAssistant.value.thinkingBudget) {
                        Icon(
                            painterResource(CommonIcons.CHECK_18),
                            null,
                            modifier = Modifier.padding(horizontal = 5.dp)
                        )
                    }

                    Text(
                        text = itemValue,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}