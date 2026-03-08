package zouitel.neighborhood.jotting.ui

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import zouitel.neighborhood.video.VideoPlayer
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.ui.getAverageColorFromImage
import kotlin.math.absoluteValue

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(UnstableApi::class)
@Composable
fun QuickMediaTop(data: MutableState<Data>, onDelete: (Media) -> Unit) {
    val pagerState = rememberPagerState { data.value.medias.size }
    var avgColor by remember { mutableStateOf(Color.Black) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(data.value.medias, pagerState.currentPage) {
        if (data.value.medias.size > 1) {
            while (true) {
                delay(7000)
                val nextPage = (pagerState.currentPage + 1) % data.value.medias.size
                pagerState.scrollToPage(page = nextPage)
            }
        }
    }

    if (data.value.medias.isNotEmpty()) {
        val containerColor = data.value.background?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
        Box(contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier
                    .dropShadow(
                        MaterialTheme.shapes.large,
                        Shadow(
                            20.dp,
                            color = avgColor,
                            spread = 15.dp,
                            alpha = .4f
                        )
                    ),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = containerColor),
            ) {
                HorizontalPager(state = pagerState) { index ->
                    scope.launch {
                        avgColor = getAverageColorFromImage(
                            context,
                            data.value.medias[pagerState.currentPage].uri
                        )
                    }
                    val pageOffset = (
                            (pagerState.currentPage - index) + pagerState
                                .currentPageOffsetFraction
                            ).absoluteValue

                    Card(
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(),
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = 1f - pageOffset.coerceIn(0f, 1f)
                                scaleY = 1f - (pageOffset * .15f)
                                scaleX = 1f - (pageOffset * .15f)
                            }
                            .fillMaxWidth()
                            .heightIn(100.dp, 220.dp)
                            .background(containerColor)
                            .combinedClickable(
                                onLongClick = { onDelete(data.value.medias[index]) }
                            ) {},
                    ) {
                        when (data.value.medias[index].isVideo) {
                            true -> {
                                VideoPlayer(
                                    uri = data.value.medias[index].uri.toUri(),
                                    {}
                                ) {}
                            }
                            else -> {
                                AsyncImage(
                                    model = data.value.medias[index].uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (data.value.medias.size > 1) {
                Row(
                    Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(data.value.medias.size) { iteration ->
                        val color by animateColorAsState(
                            targetValue = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(
                                alpha = 0.5f
                            ),
                            animationSpec = tween(durationMillis = 300),
                            label = "dotColorAnimation"
                        )
                        val width by animateDpAsState(
                            targetValue = if (pagerState.currentPage == iteration) 20.dp else 8.dp,
                            animationSpec = tween(durationMillis = 300),
                            label = "dotWidthAnimation"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .height(8.dp)
                                .width(width)
                        )
                    }
                }
            }
        }
    }
}