package zouitel.neighborhood.assistant.ui.ai_model

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowColumn
import androidx.compose.foundation.layout.ContextualFlowColumnOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.assistant.utils.SwipableModel
import zouitel.neighborhood.ui.CommonColors

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
internal fun Models(
    assistantModel: AssistantScreenModel,
    modelsModel: ModelsScreenModel,
    isCollapsed: Boolean = false,
    onEdit: (Model) -> Unit,
    onDelete: (Model) -> Unit
) {

    val assistant by remember(assistantModel, assistantModel::observe).collectAsState()
    val models by remember(modelsModel, modelsModel::observeAll).collectAsState(emptyList())
    var columnHeight by remember { mutableStateOf(300.dp) }

    // Cache current model with derivedStateOf for automatic smart invalidation
    val currentModel by remember {
        derivedStateOf {
            models.find { model -> model.uid == assistant?.currentModelUid }
        }
    }

    // Cache sorted models list with derivedStateOf to prevent unnecessary sorting
    val sortedModels by remember {
        derivedStateOf {
            models.sortedByDescending { it == currentModel }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        ContextualFlowColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(0.dp, if (isCollapsed) columnHeight else Dp.Unspecified),
            itemCount = sortedModels.size,
            maxLines = if (isCollapsed) 1 else Int.MAX_VALUE,
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
            itemHorizontalAlignment = Alignment.CenterHorizontally,
            overflow = if (isCollapsed) {
                ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                    expandIndicator = {
                        ElevatedFilterChip(
                            selected = true,
                            shape = CircleShape,
                            onClick = { columnHeight = Dp.Unspecified },
                            label = {
                                Text(
                                    text = "+${totalItemCount - shownItemCount} more",
//                                    color = Color.White
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CommonColors.prime,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    collapseIndicator = {
                        if (columnHeight == Dp.Unspecified) {
                            ElevatedFilterChip(
                                selected = true,
                                shape = CircleShape,
                                onClick = { columnHeight = 300.dp },
                                label = {
                                    Text(
                                        text = "less",
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                        }
                    }
                )
            } else {
                ContextualFlowColumnOverflow.Clip
            }
        ) { index ->
            val model = sortedModels.getOrNull(index) ?: return@ContextualFlowColumn
            SwipableModel(
                model = model,
                isCurrent = model == currentModel,
                onEdit = { onEdit(model) },
                onDelete = { onDelete(model) }
            ) {
                assistantModel.upsert(
                    assistant?.copy(currentModelUid = model.uid) ?: return@SwipableModel
                )
            }
        }
    }
}