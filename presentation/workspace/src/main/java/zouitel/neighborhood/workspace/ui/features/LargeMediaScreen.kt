package zouitel.neighborhood.workspace.ui.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import com.cherry.lib.doc.DocViewerActivity
import com.cherry.lib.doc.bean.DocSourceType
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.print.printDocumentFromUri
import zouitel.neighborhood.print.printImageFromUri
import zouitel.neighborhood.ui.CommonIcons.MENU_DOTS
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.video.VideoPlayer
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.MediaType
import zouitel.neighborhood.workspace.ui.features.details.DocumentDetails
import zouitel.neighborhood.workspace.ui.features.details.ImageDetails
import zouitel.neighborhood.workspace.ui.features.details.MediaActionsMenu
import zouitel.neighborhood.workspace.ui.features.details.VideoDetails
import kotlin.random.Random

data class LargeMediaScreen(val index: Int = 0, val data: MutableState<Data>) : Screen {

    @Composable
    override fun Content() {
        MediaLarge(getScreenModel())
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MediaLarge(settingsModel: SettingsScreenModel) {
        val vibe = LocalHapticFeedback.current
        val context = LocalContext.current
        val bottomNav = LocalBottomSheetNavigator.current

        BackHandler { bottomNav.hide() }

        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val scope = rememberCoroutineScope()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()

        // ── Carousel state replaces rememberPagerState ────────────────────────
        val carouselState = rememberCarouselState(initialItem = index) { data.value.medias.size }
        // Convenience alias — mirrors pagerState.currentPage usage throughout
        val currentPage = carouselState.currentItem

        val menuExpanded = remember { mutableStateOf(false) }
        val deletedMedia = remember { mutableStateListOf<Pair<Media, Int>>() }
        val jobCounter = remember { mutableIntStateOf(0) }
        val snackbarState = remember { SnackbarHostState() }

        var imageDetailsSheet by remember { mutableStateOf(false) }
        var videoDetailsSheet by remember { mutableStateOf(false) }
        var documentDetailsSheet by remember { mutableStateOf(false) }
        val mediaDeletedResource = stringResource(R.string.media_deleted)
        val undoResource = stringResource(R.string.action_undo)

        // Used to size each carousel item at full screen width (full-screen viewer feel)
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarState) { snackData ->
                    Snackbar(
                        snackbarData = snackData,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = MaterialTheme.colorScheme.surface
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .animateContentSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(
                    enabled = imageDetailsSheet || videoDetailsSheet ||
                            menuExpanded.value || documentDetailsSheet
                )
                .background(
                    try {
                        Color(data.value.medias[currentPage].color)
                    } catch (_: IndexOutOfBoundsException) {
                        Color.DarkGray
                    }
                ),
            containerColor = Color.Transparent,
            topBar = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TopAppBar(
                        navigationIcon = {
                            Text(
                                text = "${currentPage.inc()} ${stringResource(R.string.of)} ${data.value.medias.size}",
                                fontSize = 19.sp,
                                modifier = Modifier.padding(20.dp)
                            )
                        },
                        title = {},
                        actions = {
                            IconButton({ menuExpanded.value = true }) {
                                Icon(painterResource(MENU_DOTS), contentDescription = null)
                            }
                            MediaActionsMenu(
                                expanded = menuExpanded,
                                onDuplicate = {
                                    data.value = data.value.copy(
                                        medias = data.value.medias.plusElement(
                                            data.value.medias[currentPage]
                                                .copy(id = Random.nextLong())
                                        )
                                    )
                                },
                                onSend = {
                                    sendMedia(
                                        context,
                                        data.value.medias[currentPage].uri.toUri()
                                    )
                                },
                                onPrint = {
                                    val media = data.value.medias[currentPage]
                                    when (media.type) {
                                        MediaType.DOCUMENT -> printDocumentFromUri(context, media.uri)
                                        else -> printImageFromUri(context, media.uri)
                                    }
                                },
                                onDetails = {
                                    when (data.value.medias[currentPage].type) {
                                        MediaType.VIDEO -> videoDetailsSheet = true
                                        MediaType.IMAGE -> imageDetailsSheet = true
                                        MediaType.DOCUMENT -> documentDetailsSheet = true
                                    }
                                },
                                onDelete = {
                                    val currentJobId = jobCounter.intValue++
                                    deletedMedia.add(
                                        Pair(data.value.medias[currentPage], currentJobId)
                                    )
                                    val mediaPair =
                                        deletedMedia.firstOrNull { it.second == currentJobId }
                                    data.value = data.value.copy(
                                        medias = data.value.medias.minusElement(
                                            mediaPair?.first ?: return@MediaActionsMenu
                                        )
                                    )
                                    scope.launch {
                                        snackbarState.currentSnackbarData?.dismiss()
                                        val result = snackbarState.showSnackbar(
                                            mediaDeletedResource, undoResource, true,
                                            SnackbarDuration.Long
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            data.value = data.value.copy(
                                                medias = data.value.medias
                                                    .plusElement(mediaPair!!.first)
                                            )
                                        }
                                        deletedMedia.remove(mediaPair)
                                    }
                                }
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        ) { paddingValues ->
            Box {
                // ── HorizontalUncontainedCarousel replaces HorizontalPager ────
                // Each item fills the screen width; one item peeks at the edge,
                // giving the classic Material 3 carousel look while preserving
                // the full-screen media viewer UX.
                HorizontalUncontainedCarousel(
                    state = carouselState,
                    itemWidth = screenWidth,
                    itemSpacing = 8.dp,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -20) {
                                    when (data.value.medias.getOrNull(currentPage)?.type) {
                                        MediaType.VIDEO -> videoDetailsSheet = true
                                        MediaType.IMAGE -> imageDetailsSheet = true
                                        MediaType.DOCUMENT -> documentDetailsSheet = true
                                        null -> Unit
                                    }
                                }
                                if (dragAmount > 20) bottomNav.hide()
                            }
                        }
                ) { page ->
                    if (page >= data.value.medias.size) {
                        Spacer(Modifier.fillMaxSize())
                        return@HorizontalUncontainedCarousel
                    }

                    val media = data.value.medias[page]

                    when (media.type) {

                        // ── Video ─────────────────────────────────────────────
                        MediaType.VIDEO -> {
                            VideoPlayer(
                                uri = media.uri.toUri(),
                                // maskClip must be applied inside a CarouselItemScope
                                // to get the animated clip/mask effect from Material 3
                                modifier = Modifier
                                    .fillMaxSize()
                                    .maskClip(MaterialTheme.shapes.extraLarge),
                                onLongClock = {
                                    vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            ) {}
                        }

                        // ── Document — first-page thumbnail; tap = open viewer ─
                        MediaType.DOCUMENT -> {
                            DocumentThumbnail(
                                filePath = media.uri,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .maskClip(MaterialTheme.shapes.extraLarge),
                                onClick = {
                                    (context as? AppCompatActivity)?.let { activity ->
                                        DocViewerActivity.launchDocViewer(
                                            activity,
                                            DocSourceType.PATH,
                                            media.uri
                                        )
                                    }
                                },
                                onLongClick = {
                                    vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                    documentDetailsSheet = true
                                }
                            )
                        }

                        // ── Image ─────────────────────────────────────────────
                        MediaType.IMAGE -> {
                            ZoomableAsyncImage(
                                model = media.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .maskClip(MaterialTheme.shapes.extraLarge)
                                    .combinedClickable(
                                        onLongClick = {
                                            vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ) {}
                            )
                        }
                    }
                }

                // ── Detail sheets ─────────────────────────────────────────────
                if (imageDetailsSheet) {
                    ImageDetails(data.value.medias[currentPage].uri) {
                        imageDetailsSheet = false
                    }
                }
                if (videoDetailsSheet) {
                    VideoDetails(data.value.medias[currentPage].uri) {
                        videoDetailsSheet = false
                    }
                }
                if (documentDetailsSheet) {
                    DocumentDetails(data.value.medias[currentPage].uri) {
                        documentDetailsSheet = false
                    }
                }
            }
        }
    }

    private fun sendMedia(context: Context, uri: Uri) {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        share.resolveActivity(context.packageManager)?.let {
            context.startActivity(Intent.createChooser(share, "Share Media"))
        } ?: Toast.makeText(context, "No app found to share media!", Toast.LENGTH_SHORT).show()
    }
}