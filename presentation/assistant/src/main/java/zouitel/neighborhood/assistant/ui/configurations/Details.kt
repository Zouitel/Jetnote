package zouitel.neighborhood.assistant.ui.configurations

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import zouitel.neighborhood.ai.provider.Modality
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.provider.ModelAbility
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons

class Details(val currentMode: Model) {
    @Composable
    private fun Modalities(model: Model) {
        var showKey by remember { mutableStateOf(false) }
        AssistChip(
            modifier = Modifier.padding(horizontal = 3.dp),
            onClick = { showKey = !showKey },
            label = {
                if (showKey) {
                    Text(
                        "Modalities",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    model.inputModalities.fastForEach { modality ->
                        Icon(
                            when (modality) {
                                Modality.TEXT -> painterResource(CommonIcons.TEXT)
                                Modality.IMAGE -> painterResource(CommonIcons.PICTURE)
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(LocalTextStyle.current.lineHeight.value.dp)
                                .padding(1.dp)
                        )
                    }
                    Icon(
                        painterResource(CommonIcons.ARROW_RIGHT),
                        contentDescription = null,
                        modifier = Modifier.size(LocalTextStyle.current.lineHeight.value.dp)
                    )
                    model.outputModalities.fastForEach { modality ->
                        Icon(
                            when (modality) {
                                Modality.TEXT -> painterResource(CommonIcons.TEXT)
                                Modality.IMAGE -> painterResource(CommonIcons.PICTURE)
                            },
                            contentDescription = null,
                            modifier = Modifier
                                .size(LocalTextStyle.current.lineHeight.value.dp)
                                .padding(1.dp)
                        )
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (showKey) CommonColors.prime else MaterialTheme.colorScheme.surface,
                labelColor = Color.LightGray,
            ),
            border = BorderStroke(1.dp, CommonColors.prime)
        )
    }

    @Composable
    fun Tools(model: Model) {
        model.abilities.find { it == ModelAbility.TOOL }?.let {
            var showKey by remember { mutableStateOf(false) }
            AssistChip(
                modifier = Modifier.padding(horizontal = 3.dp),
                onClick = { showKey = !showKey },
                label = {
                    if (showKey) {
                        Text(
                            "Tools",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Image(
                            painterResource(R.drawable.tools),
                            "tools"
                        )
                    }
                },
                shape = MaterialTheme.shapes.large,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (showKey) CommonColors.prime else MaterialTheme.colorScheme.surface,
                    labelColor = Color.LightGray,
                ),
                border = BorderStroke(1.dp, CommonColors.prime)
            )
        } ?: return
    }

    @Composable
    fun Reasoning(model: Model) {
        model.abilities.find { it == ModelAbility.REASONING }?.let {
            var showKey by remember { mutableStateOf(false) }
            AssistChip(
                modifier = Modifier.padding(horizontal = 3.dp),
                onClick = { showKey = !showKey },
                label = {
                    if (showKey) {
                        Text(
                            "Reasoning",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Image(
                            painterResource(R.drawable.deep_learning),
                            "reasoning"
                        )
                    }
                },
                shape = MaterialTheme.shapes.large,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (showKey) CommonColors.prime else MaterialTheme.colorScheme.surface,
                    labelColor = Color.LightGray,
                ),
                border = BorderStroke(1.dp, CommonColors.prime)
            )
        } ?: return
    }

    @Composable
    private fun Name(model: Model) {
        var showKey by remember { mutableStateOf(false) }
        AssistChip(
            modifier = Modifier.padding(horizontal = 3.dp),
            onClick = { showKey = !showKey },
            label = {
                    Text(
                        if (showKey) "Model name" else model.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
            },
            shape = MaterialTheme.shapes.large,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (showKey) CommonColors.prime else MaterialTheme.colorScheme.surface,
                labelColor = Color.LightGray,
            ),
            border = BorderStroke(1.dp, CommonColors.prime)
        )
    }

    @Composable
    fun Properties() {
        FlowRow(Modifier.animateContentSize()) {
            Name(currentMode)
            Modalities(currentMode)
            Tools(currentMode)
            Reasoning(currentMode)
        }
    }
}
