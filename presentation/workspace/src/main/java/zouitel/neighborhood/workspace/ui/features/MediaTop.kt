package zouitel.neighborhood.workspace.ui.features

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import coil.compose.SubcomposeAsyncImage
import com.artifex.mupdf.fitz.Document
import com.artifex.mupdf.fitz.Matrix
import com.artifex.mupdf.fitz.android.AndroidDrawDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import zouitel.neighborhood.ui.getAverageColorFromImage
import zouitel.neighborhood.video.VideoPlayer
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.MediaType
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun MediaTop(data: MutableState<Data>) {
    val vibe= LocalHapticFeedback.current
    val bottomNav = LocalBottomSheetNavigator.current
    val context = LocalContext.current

    val carouselState = rememberCarouselState { data.value.medias.size }
    val currentPage = carouselState.currentItem

    var avgColor by remember { mutableStateOf(Color.Black) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    LaunchedEffect(Unit) {
        if (data.value.medias.isNotEmpty()) {
            delay(50)
            carouselState.scrollToItem(data.value.medias.size - 1)
        }
    }

    LaunchedEffect(data.value.medias.size) {
        if (data.value.medias.isNotEmpty()) {
            delay(50.milliseconds)
            carouselState.animateScrollToItem(data.value.medias.size - 1)
        }
    }

    LaunchedEffect(currentPage, data.value.medias) {
        val media = data.value.medias.getOrNull(currentPage)
        if (media != null && currentPage in data.value.medias.indices) {
            avgColor = getAverageColorFromImage(context, media.uri)
        }
    }

    if (data.value.medias.isNotEmpty()) {
        val containerColor = runCatching {
            data.value.medias.getOrNull(currentPage)?.color
                ?.let { Color(it) } ?: Color.DarkGray
        }.getOrElse { Color.DarkGray }

        Box(contentAlignment = Alignment.BottomCenter) {
            Card(
                modifier = Modifier.dropShadow(
                    MaterialTheme.shapes.large,
                    Shadow(20.dp, color = avgColor, spread = 15.dp, alpha = .4f)
                ),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = containerColor)
            ) {
                HorizontalUncontainedCarousel(
                    state= carouselState,
                    itemWidth = screenWidth,
                    itemSpacing = 8.dp
                ) { index ->
                    val pageOffset = ((currentPage - index) + 0f).absoluteValue
                    val media = data.value.medias.getOrNull(index)

                    Card(
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(),
                        modifier = Modifier
                            .graphicsLayer {
                                alpha  = 1f - pageOffset.coerceIn(0f, 1f)
                                scaleY = 1f - (pageOffset * .15f)
                                scaleX = 1f - (pageOffset * .15f)
                            }
                            .maskClip(MaterialTheme.shapes.large)
                            .fillMaxWidth()
                            .heightIn(100.dp, 220.dp),
                        colors = CardDefaults.cardColors(containerColor)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (media?.type) {
                                MediaType.VIDEO -> {
                                    VideoPlayer(
                                        uri = media.uri.toUri(),
                                        onLongClock = {
                                            vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                            it.show(LargeMediaScreen(index, data))
                                        }
                                    ) { it.show(LargeMediaScreen(index, data)) }
                                }

                                MediaType.DOCUMENT -> {
                                    DocumentThumbnail(
                                        filePath = media.uri,
                                        onClick = { bottomNav.show(LargeMediaScreen(index, data)) },
                                        onLongClick = {
                                            vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                            bottomNav.show(LargeMediaScreen(index, data))
                                        }
                                    )
                                }

                                MediaType.IMAGE, null -> {
                                    media?.let {
                                        SubcomposeAsyncImage(
                                            model = it.uri,
                                            contentDescription = null,
                                            loading = {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) { CircularProgressIndicator() }
                                            },
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .combinedClickable(
                                                    onLongClick = {
                                                        vibe.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        bottomNav.show(LargeMediaScreen(index, data))
                                                    }
                                                ) { bottomNav.show(LargeMediaScreen(index, data)) }
                                        )
                                    }
                                }
                            }

                            // ── Bottom scrim + caption overlay ─────────────────
//                            if (media != null) {
//                                val statNumber = remember(index) { (index + 1).toString() }
//                                val statLabel = remember(media.type) {
//                                    when (media.type) {
//                                        MediaType.VIDEO -> "Video"
//                                        MediaType.DOCUMENT -> "Document"
//                                        MediaType.IMAGE, null -> "Photo"
//                                    }
//                                }
//
//                                Box(
//                                    modifier = Modifier
//                                        .align(Alignment.BottomStart)
//                                        .fillMaxWidth()
//                                        .fillMaxHeight(0.55f)
//                                        .background(
//                                            Brush.verticalGradient(
//                                                colors = listOf(
//                                                    Color.Transparent,
//                                                    Color.Black.copy(alpha = 0.75f)
//                                                )
//                                            )
//                                        )
//                                )
//
//                                Column(
//                                    modifier = Modifier
//                                        .align(Alignment.BottomStart)
//                                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
//                                ) {
//                                    Text(
//                                        text = statNumber,
//                                        color = Color.White,
//                                        style = MaterialTheme.typography.displaySmall,
//                                        fontWeight = FontWeight.Bold
//                                    )
//                                    Text(
//                                        text = statLabel,
//                                        color = Color.White.copy(alpha = 0.9f),
//                                        style = MaterialTheme.typography.titleMedium
//                                    )
//                                }
//                            }
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
                            targetValue = if (currentPage == iteration) Color.White
                            else Color.White.copy(alpha = 0.5f),
                            animationSpec = tween(durationMillis = 300),
                            label = "dotColorAnimation"
                        )
                        val width by animateDpAsState(
                            targetValue = if (currentPage == iteration) 20.dp else 8.dp,
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

// ── Document thumbnail ────────────────────────────────────────────────────────
@Composable
fun DocumentThumbnail(
    filePath : String,
    modifier : Modifier = Modifier,
    onClick : () -> Unit,
    onLongClick: () -> Unit
) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, filePath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val doc = Document.openDocument(filePath)
                val page = doc.loadPage(0)
                val matrix = Matrix(2f, 2f)
                val bmp = AndroidDrawDevice.drawPage(page, matrix)
                doc.destroy()
                bmp
            }.getOrNull()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .combinedClickable(onLongClick = onLongClick, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
    }
}