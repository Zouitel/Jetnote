package zouitel.neighborhood.workspace.ui.features

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.print.printImageFromUri
import zouitel.neighborhood.ui.CommonIcons.MENU_DOTS
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.video.VideoPlayer
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.ui.features.details.ImageDetails
import zouitel.neighborhood.workspace.ui.features.details.MediaActionsMenu
import zouitel.neighborhood.workspace.ui.features.details.VideoDetails
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import kotlin.random.Random

data class LargeMediaScreen(val index: Int = 0, val data: MutableState<Data>): Screen {
    @Composable
    override fun Content() {
        MediaLarge(getScreenModel())
    }

    @Composable
    private fun MediaLarge(settingsModel: SettingsScreenModel) {

        val vibe = LocalHapticFeedback.current
        val context = LocalContext.current
        val bottomNav = LocalBottomSheetNavigator.current

        BackHandler {
            bottomNav.hide()
        }

        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val scope = rememberCoroutineScope()
        val settings by remember(settingsModel,settingsModel::observe).collectAsState()

        val pagerState = rememberPagerState(index) { data.value.medias.size }
        val menuExpanded = remember { mutableStateOf(false) }
        val deletedMedia = remember { mutableStateListOf<Pair<Media, Int>>() }
        val jobCounter = remember { mutableIntStateOf(0) }
        val modalSheetState = rememberModalBottomSheetState(true)
        val scaffoldState = rememberBottomSheetScaffoldState(modalSheetState)
        val snackbarState = remember { SnackbarHostState() }
        var imageDetailsSheet by remember { mutableStateOf(false) }
        var videoDetailsSheet by remember { mutableStateOf(false) }

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarState) { data ->
                    Snackbar(
                        snackbarData = data,
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
                .cloudy(enabled = imageDetailsSheet || videoDetailsSheet || menuExpanded.value)
                .background(
                    try {
                        Color(data.value.medias[pagerState.currentPage].color)
                    } catch (_: IndexOutOfBoundsException) {
                        Color.DarkGray
                    }
                ),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        Text(
                            text = "${pagerState.currentPage.inc()} of ${pagerState.pageCount}",
                            fontSize = 19.sp,
                            modifier = Modifier.padding(20.dp)
                        )
                    },
                    title = {},
                    actions = {
                        IconButton({
                            menuExpanded.value = true
                        }) {
                            Icon(
                                painter = painterResource(MENU_DOTS),
                                contentDescription = null
                            )
                        }
                        MediaActionsMenu(
                            expanded = menuExpanded,
                            onDuplicate = {
                                data.value = data.value.copy(
                                    medias = data.value.medias.plusElement(
                                        data.value.medias[pagerState.currentPage].copy(
                                            id = Random.nextLong()
                                        )
                                    )
                                )
                            },
                            onSend = {
                                sendMedia(
                                    context,
                                    data.value.medias[pagerState.currentPage].uri.toUri()
                                )
                            },
                            onPrint = {
                                printImageFromUri(
                                    context,
                                    data.value.medias[pagerState.currentPage].uri
                                )
                            },
                            onDetails = {
                                if (data.value.medias[pagerState.currentPage].isVideo){
                                    videoDetailsSheet = true
                                } else {
                                    imageDetailsSheet = true
                                }
                            },
                            onDelete = {
                                val currentJobId = jobCounter.intValue++
                                deletedMedia.add(
                                    Pair(
                                        data.value.medias[pagerState.currentPage],
                                        currentJobId
                                    )
                                )
                                val mediaPair =
                                    deletedMedia.firstOrNull { it.second == currentJobId }
                                data.value = data.value.copy(
                                    medias = data.value.medias.minusElement(
                                        mediaPair?.first ?: return@MediaActionsMenu
                                    )
                                )

                                scope.launch {
                                    scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = scaffoldState.snackbarHostState.showSnackbar(
                                        message = "${mediaPair.first.title} deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )


                                    if (result == SnackbarResult.ActionPerformed) {
                                        mediaPair.first.let { media ->
                                            data.value = data.value.copy(
                                                medias = data.value.medias.plusElement(media)
                                            )
                                        }
                                    }
                                    mediaPair.let { deletedMedia.remove(it) }
                                }
                            },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Box {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                if (dragAmount < -20) {
                                    if (data.value.medias[pagerState.currentPage].isVideo) {
                                        videoDetailsSheet = true
                                    } else {
                                        imageDetailsSheet = true
                                    }
                                }
                                if (dragAmount > 20) bottomNav.hide()
                            }
                        }
                ) { page ->
                    if (page < data.value.medias.size) {
                        val media = data.value.medias[page]
                        when (media.isVideo) {
                            true -> {
                                VideoPlayer(
                                    uri = media.uri.toUri(),
                                    onLongClock = {
                                        vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ) {}
                            }

                            else -> {
                                ZoomableAsyncImage(
                                    model = media.uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .combinedClickable(
                                            onLongClick = {
                                                vibe.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        ) {}
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                }

                if (imageDetailsSheet) {
                    ImageDetails(data.value.medias[pagerState.currentPage].uri) {
                        imageDetailsSheet = false
                    }
                }
                if (videoDetailsSheet) {
                    VideoDetails(data.value.medias[pagerState.currentPage].uri) {
                        videoDetailsSheet = false
                    }
                }
            }
        }
    }

    private fun sendMedia(context: Context, uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(shareIntent, "Share Media")

        shareIntent.resolveActivity(context.packageManager) ?. let {
            context.startActivity(chooserIntent)
        } ?: let {
            Toast.makeText(context, "No app found to share media item!", Toast.LENGTH_SHORT).show()
        }
    }
}
