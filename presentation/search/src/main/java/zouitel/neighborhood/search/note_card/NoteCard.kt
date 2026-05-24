@file:Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")

package zouitel.neighborhood.search.note_card

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import cafe.adriel.voyager.navigator.LocalNavigator
import coil.compose.AsyncImage
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.audio.utils.SmallAudioPlayer
import zouitel.neighborhood.link.ui.LinkCard
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.search.utils.normalNotePath
import zouitel.neighborhood.search.utils.prioritizedNotePath
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons.CIRCLE_18
import zouitel.neighborhood.ui.CommonIcons.GRIP_DOTS
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.video.SmallVideoPlayer
import zouitel.neighborhood.workspace.model.MediaType
import zouitel.neighborhood.workspace.model.Note
import zouitel.neighborhood.workspace.ui.features.DocumentThumbnail
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreen

@Composable
fun NoteCard(
    settingsModel: SettingsScreenModel,
    audioModel: AudioScreenModel,
    linkModel: LinkScreenModel,
    searchModel: SearchScreenModel,
    note: Note,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier
) {
    Card(
        settingsModel = settingsModel,
        audioModel = audioModel,
        linkModel = linkModel,
        noteEntity = note,
        searchModel = searchModel,
        isDragging = isDragging,
        dragModifier = dragModifier
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun Card(
    settingsModel: SettingsScreenModel,
    linkModel: LinkScreenModel,
    audioModel: AudioScreenModel,
    noteEntity: Note,
    searchModel: SearchScreenModel,
    isDragging: Boolean = false,
    dragModifier: Modifier = Modifier
) {
    val note = noteEntity.data
    val labels = noteEntity.tags

    val haptic = LocalHapticFeedback.current
    val navigator = LocalNavigator.current

    val elevation by animateDpAsState(if (isDragging) 16.dp else 0.dp, label = "drag_elev")

    val uiState by remember(searchModel, searchModel::uiState).collectAsState()
    val observerLinks by remember(linkModel, linkModel::observeAll).collectAsState()
    val filteredLinks = observerLinks.filter { it.uid == note.uid }
    val pagerState = rememberPagerState { note.medias.size }
    val animatedSpread = remember { Animatable(0f) }
    val audios = note.audios

    LaunchedEffect(uiState.selectedNotes.contains(note)) {
        animatedSpread.animateTo(
            targetValue = if (uiState.selectedNotes.contains(note)) 15f else 0f,
            animationSpec = tween(durationMillis = 150, easing = LinearEasing)
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        searchModel.updateSelectedNotes(note)
                        searchModel.updateSelection(true)
                    }
                ) {
                    if (uiState.isSelection.not()) {
                        navigator?.push(WorkplaceScreen(note, false))
                    } else {
                        when {
                            uiState.selectedNotes.contains(note).not() ->
                                uiState.selectedNotes.add(note)
                            else ->
                                uiState.selectedNotes.remove(note)
                        }
                    }
                    uiState.selectedNotes.ifEmpty {
                        searchModel.updateSelection(false)
                    }
                }
                .drawBehind {
                    if (note.priority == 0) normalNotePath(note)
                    else prioritizedNotePath(note)
                },
            shape = AbsoluteRoundedCornerShape(15.dp),
            border = if (uiState.selectedNotes.contains(note)) {
                BorderStroke(3.dp, CommonColors.prime)
            } else {
                when (note.background == MaterialTheme.colorScheme.surface.toArgb()) {
                    true -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)
                    false -> BorderStroke(0.dp, Color.Transparent)
                }
            },
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            colors    = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            if (note.medias.isNotEmpty()) {
                val containerColor = runCatching {
                    Color(note.medias[pagerState.currentPage].color)
                }.getOrElse { Color.DarkGray }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(100.dp, 220.dp)
                        .background(note.background?.let { Color(it) } ?: MaterialTheme.colorScheme.surface)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.background(
                            note.background?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                        )
                    ) { index ->
                        Card(
                            modifier = Modifier.dropShadow(
                                RoundedCornerShape(20.dp),
                                Shadow(
                                    10.dp,
                                    color = Color(note.medias[index].color),
                                    spread = 9.dp,
                                    alpha = .5f
                                )
                            ),
                            shape = AbsoluteRoundedCornerShape(15.dp),
                            elevation = CardDefaults.cardElevation(),
                            colors = CardDefaults.cardColors(containerColor)
                        ) {
                            val media = note.medias[index]
                            when (media.type) {
                                MediaType.VIDEO -> SmallVideoPlayer(media.uri.toUri())
                                MediaType.DOCUMENT -> DocumentThumbnail(
                                    filePath = media.uri,
                                    onClick = { navigator?.push(WorkplaceScreen(note, false)) },
                                    onLongClick = {}
                                )

                                MediaType.IMAGE -> AsyncImage(
                                    model = media.uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (note.medias.count() > 1) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopStart).padding(3.dp),
                            contentColor = Color.White.copy(alpha = .5f),
                            containerColor = Color.Black.copy(alpha = .5f)
                        ) {
                            Text(text = "${pagerState.currentPage + 1}/${note.medias.count()}")
                        }
                    }
                }
            }

            if (note.title.isNotEmpty()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = note.textColor?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(3.dp)
                )
            }

            when {
                note.blocks.isNotEmpty() -> {
                    BlocksPreview(
                        blocks = note.blocks,
                        textColor = note.textColor?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 3.dp, end = 3.dp, bottom = 7.dp),
                    )
                }
            }

            audios.fastFirstOrNull {
                SmallAudioPlayer(audios.size, settingsModel, audioModel, it)
                true
            }

            ContextualFlowRow(
                itemCount = labels.size,
                maxLines  = 1,
                overflow  = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                    expandIndicator = {
                        AssistChip(
                            modifier = Modifier.alpha(.5f),
                            border = BorderStroke(0.dp, Color.Transparent),
                            onClick = { },
                            label = { Text("+${totalItemCount - shownItemCount}", fontSize = 13.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = note.textColor?.let { Color(it) }
                                    ?: MaterialTheme.colorScheme.onSurface
                            ),
                            shape = CircleShape
                        )
                    },
                    collapseIndicator = { }
                )
            ) { index ->
                AssistChip(
                    modifier = Modifier.alpha(.8f).padding(start = 5.dp, end = 5.dp),
                    border = BorderStroke(0.dp, Color.Transparent),
                    onClick = { },
                    label = { Text(labels.getOrNull(index)?.label ?: "", fontSize = 11.sp) },
                    leadingIcon = {
                        labels.getOrNull(index)?.color?.let {
                            Icon(
                                painter = painterResource(id = CIRCLE_18),
                                contentDescription = null,
                                tint = Color(it),
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = note.textColor?.let { Color(it) }
                            ?: MaterialTheme.colorScheme.onSurface,
                        containerColor = Color.DarkGray.copy(alpha = .4f)
                    ),
                    shape = CircleShape
                )
            }

            filteredLinks.apply {
                if (isNotEmpty()) LinkCard(
                    first(),
                    count(),
                    note.background ?: MaterialTheme.colorScheme.surface.toArgb(),
                    note.textColor  ?: MaterialTheme.colorScheme.onSurface.toArgb()
                )
            }

        }
        if (uiState.isSelection) {
            IconButton(
                {},
                Modifier.then(dragModifier)
                    .align(Alignment.TopEnd),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = note.background?.let { Color(it) } ?: Color.Unspecified,
                    contentColor = Color.White.copy(alpha = .5f)
                )
            ) {
                Icon(
                    painter = painterResource(id = GRIP_DOTS),
                    contentDescription = null,
                    tint = note.textColor?.let { Color(it) }
                        ?: MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}