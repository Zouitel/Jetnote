package zouitel.neighborhood.workspace.ui.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ui.AnimatedCircleCheckbox
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TaskItem(
    currentData: MutableState<Data>,
    task: Task,
    onEdit: (BottomSheetNavigator) -> Unit,
    onDelete: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxSwipeDistance = -300f

    val bottomNav = LocalBottomSheetNavigator.current

    Box(modifier = Modifier.fillMaxWidth()) {
        // Background actions
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        offsetX.animateTo(0f, tween(300))
                        onEdit(bottomNav)
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    painter = painterResource(CommonIcons.PENCIL),
                    contentDescription = "Edit"
                )
            }
            
            Spacer(modifier = Modifier.width(3.dp))
            
            IconButton(
                onClick = {
                    scope.launch {
                        offsetX.animateTo(0f, tween(300))
                        delay(100)
                        onDelete()
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(
                    painter = painterResource(CommonIcons.TRASH),
                    contentDescription = "Delete"
                )
            }
            
            Spacer(modifier = Modifier.width(3.dp))
        }

        // Foreground content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(45.dp)
                .graphicsLayer {
                    shadowElevation = if (offsetX.value != 0f) 12.dp.toPx() else 0f
                    translationX = offsetX.value
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < maxSwipeDistance / 2) {
                                    offsetX.animateTo(maxSwipeDistance, tween(300))
                                } else {
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(maxSwipeDistance, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
                .padding(start = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = currentData.value.background?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedCircleCheckbox(
                    checked = task.isDone,
                    onCheckedChange = {
                        currentData.value = currentData.value.copy(
                            tasks = currentData.value.tasks.map { t ->
                                if (t.id == task.id) {
                                    t.copy(isDone = it)
                                } else {
                                    t
                                }
                            }
                        )
                    },
                    background = currentData.value.background?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.surface,
                    textColor = currentData.value.textColor?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = task.item ?: "",
                    Modifier.padding(start = 5.dp),
                    style = TextStyle(
                        fontSize = 19.sp,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (task.isDone) Color.Gray else currentData.value.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}