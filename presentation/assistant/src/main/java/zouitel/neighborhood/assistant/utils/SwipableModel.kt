package zouitel.neighborhood.assistant.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SwipableModel(
    model: Model,
    isCurrent: Boolean,
    onEdit: (BottomSheetNavigator) -> Unit,
    onDelete: () -> Unit,
    onClicked: () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxSwipeDistance = -300f

    val bottomNav = LocalBottomSheetNavigator.current

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
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
                    contentDescription = "edit"
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
                    contentDescription = "delete"
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
                .clickable { onClicked() }
                .clip(MaterialTheme.shapes.extraLarge)
                .align(Alignment.Center),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrent) CommonColors.prime
                else MaterialTheme.colorScheme.surfaceContainer,
                contentColor = if (isCurrent) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(45.dp).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    isLoading = false,
                    logo = model.logoUrl,
                    modelName = model.displayName
                ) { }

                Text(
                    text = model.displayName,
                    style = TextStyle(
                        fontSize = 19.sp,
                    )
                )
            }
        }
    }
}