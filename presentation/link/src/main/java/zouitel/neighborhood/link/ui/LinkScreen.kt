package zouitel.neighborhood.link.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowColumn
import androidx.compose.foundation.layout.ContextualFlowColumnOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.link.model.Link
import zouitel.neighborhood.ui.CommonIcons
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class LinkScreen(val uid: String, val snackbarHostState: SnackbarHostState): Screen {
    @Composable
    override fun Content() {

        val scope = rememberCoroutineScope()
        val linkModel = getScreenModel<LinkScreenModel>()
        val links = remember(linkModel, linkModel::observeByUid).collectAsState()
        var columnHeight by remember { mutableStateOf(300.dp) }
        val deletedLink = remember { mutableStateListOf<Pair<Link, Int>>() }
        val jobCounter = remember { mutableIntStateOf(0) }

        LaunchedEffect(true) {
            linkModel.initializeUid(uid)
        }

        ContextualFlowColumn(
            modifier = Modifier.heightIn(0.dp, columnHeight),
            itemCount = links.value.size,
            maxLines = 1,
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
            itemHorizontalAlignment = Alignment.CenterHorizontally,
            overflow = ContextualFlowColumnOverflow.expandOrCollapseIndicator(
                expandIndicator = {
                    ElevatedFilterChip(
                        selected = true,
                        shape = CircleShape,
                        onClick = { columnHeight = Dp.Unspecified },
                        label = {
                            Text(
                                text = "+${totalItemCount - shownItemCount} more",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary
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
        ) { index ->
            val link = links.value.getOrNull(index) ?: return@ContextualFlowColumn

            Link(link) {
                val currentJobId = jobCounter.intValue++
                deletedLink.add(Pair(link, currentJobId))
                val linkPair = deletedLink.firstOrNull { it.second == currentJobId }

                linkModel.sendAction(Action.Delete(link))

                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = "${linkPair?.first?.title} deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long
                    )

                    if (result == SnackbarResult.ActionPerformed) {
                        linkPair?.first?.let { link ->
                            linkModel.sendAction(Action.Insert(link))
                        }
                    }
                    linkPair.let { deletedLink.remove(it) }
                }
            }
        }
    }

    @Composable
    private fun Link(link: Link, onDelete: () -> Unit) {
        val urlHandler = LocalUriHandler.current

        val offsetX = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val maxSwipeDistance = -300f
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(offsetX.value != 0f) {
                    IconButton(
                        modifier = Modifier.padding(horizontal = 3.dp),
                        onClick = {
                            scope.launch {
                                urlHandler.openUri(link.url)
                                delay(100.milliseconds)
                                offsetX.animateTo(0f, tween(300))
                            }
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            painter = painterResource(CommonIcons.LINK),
                            contentDescription = "Open link"
                        )
                    }
                }

                AnimatedVisibility(offsetX.value != 0f) {
                    IconButton(
                        modifier = Modifier.padding(horizontal = 3.dp),
                        onClick = {
                            scope.launch {
                                onDelete()
                                delay(100.milliseconds)
                                offsetX.animateTo(0f, tween(300))
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
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
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
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = Color(.6f, .6f, .6f, .5f),
                    contentColor = Color.White
                )
            ) {
                Row {
                    AsyncImage(
                        modifier = Modifier.size(74.dp),
                        model = link.image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )

                    Column {
                        Text(
                            text = link.title,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 5.dp, top = 5.dp)
                        )

                        Text(
                            text = link.description,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 5.dp, top = 5.dp)
                        )
                    }
                }
            }
        }
    }
}