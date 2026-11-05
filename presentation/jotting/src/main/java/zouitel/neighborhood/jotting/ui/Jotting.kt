package zouitel.neighborhood.jotting.ui

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.audio.utils.MediumAudioPlayer
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.action.InsertBlock
import zouitel.neighborhood.markdown.state.BlockTextStates
import zouitel.neighborhood.markdown.state.rememberEditorState
import zouitel.neighborhood.markdown.theme.CascadeEditorColors
import zouitel.neighborhood.markdown.theme.CascadeEditorTheme
import zouitel.neighborhood.markdown.ui.CascadeEditor
import zouitel.neighborhood.permissions.PermissionResult
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.jotting.model.Screens
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import zouitel.neighborhood.workspace.ui.features.RecorderSheet
import zouitel.neighborhood.workspace.ui.features.ReminderSheet
import zouitel.neighborhood.workspace.ui.utils.remindersContext
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreenModel
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.ui.getAverageColorFromImage
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Jotting(
    screen: MutableState<Screens?>,
    data: MutableState<Data>,
    shareRevision: Int,
    sharedText: String?,
    incomingMedias: List<Media>,
    incomingAudios: List<Audio>,
    onShareConsumed: () -> Unit,
    onDismiss: () -> Unit
) {
    val playerModel = koinInject<AudioScreenModel>()
    val alarmModel = koinInject<AlarmManagerScreenModel>()
    val workplaceModel = koinInject<WorkplaceScreenModel>()
    val permissionsModel = koinInject<PermissionScreenModel>()
    val dataModel = koinInject<DataScreenModel>()
    val linkModel = koinInject<LinkScreenModel>()

    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val pictureUri = rememberSaveable { mutableStateOf<Uri?>(null) }
    val snackbarState = remember { SnackbarHostState() }
    val toolTipState = rememberTooltipState(isPersistent = true)

    val jottingSheetState = rememberBottomSheetState(SheetValue.Hidden)

    val showRecorderSheet = remember { mutableStateOf(false) }
    val showReminderSheet = remember { mutableStateOf(false) }
    val recorderSheetState = rememberBottomSheetState(SheetValue.Hidden)
    val reminderSheetState = rememberBottomSheetState(SheetValue.Hidden)

    val getPictureUri: () -> Uri = remember {
        {
            val uri = createMediaUri(context, "jpg")
            pictureUri.value = uri
            uri
        }
    }

    val snackBarHostState = remember { SnackbarHostState() }
    val deletedAudios = remember { mutableStateListOf<Pair<Audio, Int>>() }
    val deletedMedias = remember { mutableStateListOf<Pair<Media, Int>>() }
    val deletedReminders = remember { mutableStateListOf<Pair<Reminder, Int>>() }
    val jobCounter = remember { mutableIntStateOf(0) }

    val descriptionState = rememberTextFieldState("")
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val backgroundColorState = rememberSaveable { mutableIntStateOf(backgroundColor) }

    val textColor = contentColorFor(MaterialTheme.colorScheme.surface).toArgb()
    val textColorState = rememberSaveable { mutableIntStateOf(textColor) }

    val permissionDeniedResource = stringResource(R.string.permission_denied)
    val untitledResource = stringResource(R.string.untitled)
    val mediaDeletedResource = stringResource(R.string.media_deleted)
    val recordDeletedResource = stringResource(R.string.record_deleted)
    val undoResource = stringResource(R.string.action_undo)
    val editorState = rememberEditorState(data.value.blocks)
    LaunchedEffect(shareRevision) {
        if (shareRevision > 0) {
            sharedText
                ?.lineSequence()
                ?.filter { it.isNotBlank() }
                ?.forEach { line -> editorState.dispatch(InsertBlock(Block.paragraph(line))) }

            if (incomingMedias.isNotEmpty() || incomingAudios.isNotEmpty()) {
                data.value = data.value.copy(
                    medias = data.value.medias + incomingMedias,
                    audios = data.value.audios + incomingAudios
                )
            }
            onShareConsumed()
        }
    }
    val isImeVisible = WindowInsets.isImeVisible

    val cascadeTheme = remember(backgroundColorState.intValue, textColorState.intValue) {
        val bg = Color(backgroundColorState.intValue)
        val noteText = Color(textColorState.intValue)
        val isDark = bg.luminance() < 0.5f
        if (isDark) {
            CascadeEditorTheme.dark().copy(
                colors = CascadeEditorColors.dark().copy(text = noteText)
            )
        } else {
            CascadeEditorTheme.light().copy(
                colors = CascadeEditorColors.light().copy(text = noteText)
            )
        }
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200.milliseconds)
        focus.requestFocus()
        keyboard?.show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pictureUri.value != null) {
            scope.launch {
                val averageColor = getAverageColorFromImage(context, pictureUri.value.toString())
                data.value = data.value.copy(
                    medias = data.value.medias.plusElement(
                        Media(uri = pictureUri.value.toString(), color = averageColor.toArgb())
                    )
                )
            }
        }
    }

    LaunchedEffect(screen.value) {
        dataModel.sendAction(Action.Insert(data))
        when (screen.value) {
            Screens.NOTE_SCREEN -> playerModel.initController()
            Screens.RECORD_SCREEN -> {
                permissionsModel.requestPermission(Permission.RECORD_AUDIO).collect {
                    when (it) {
                        PermissionResult.Denied -> Toast.makeText(context, permissionDeniedResource, Toast.LENGTH_SHORT).show()
                        is PermissionResult.Error -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                        PermissionResult.Granted -> showRecorderSheet.value = true
                        PermissionResult.PermanentlyDenied -> permissionsModel.permissionDeniedHandle(snackBarHostState)
                    }
                }
            }
            Screens.PHOTO_SCREEN -> {
                scope.launch {
                    permissionsModel.requestPermission(Permission.CAMERA).collect {
                        when (it) {
                            PermissionResult.Denied -> Toast.makeText(context, permissionDeniedResource, Toast.LENGTH_SHORT).show()
                            is PermissionResult.Error -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                            PermissionResult.Granted -> cameraLauncher.launch(getPictureUri())
                            PermissionResult.PermanentlyDenied -> permissionsModel.permissionDeniedHandle(snackBarHostState)
                        }
                    }
                }
            }
            Screens.REMINDER_SCREEN -> showReminderSheet.value = true
            null -> Unit
        }
        screen.value = null
    }

    // ── Main Jotting Bottom Sheet ─────────────────────────────────────────────
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = jottingSheetState,
        containerColor = Color(backgroundColorState.intValue)
    ) {
        Surface(color = Color.Transparent) {
            val textStates = remember { BlockTextStates() }
            // Box lets the Snackbar float over content without a Scaffold slot
            Column(Modifier.fillMaxWidth()) {
                // Media thumbnails row
                QuickMediaTop(data) { media ->
                    val currentJobId = jobCounter.intValue++
                    deletedMedias.add(Pair(media, currentJobId))
                    val mediaPair = deletedMedias.firstOrNull { it.second == currentJobId }
                    data.value = data.value.copy(
                        medias = data.value.medias.minusElement(
                            mediaPair?.first ?: return@QuickMediaTop
                        )
                    )
                    scope.launch {
                        snackBarHostState.currentSnackbarData?.dismiss()
                        val result = snackBarHostState.showSnackbar(
                            message = mediaDeletedResource,
                            actionLabel = undoResource,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            data.value = data.value.copy(
                                medias = data.value.medias.plusElement(mediaPair.first)
                            )
                        }
                        deletedMedias.remove(mediaPair)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    items(data.value.audios) { audio ->
                        MediumAudioPlayer(playerModel, audio) {
                            val currentJobId = jobCounter.intValue++
                            deletedAudios.add(Pair(audio, currentJobId))
                            val audioPair = deletedAudios.firstOrNull { it.second == currentJobId }
                            data.value = data.value.copy(
                                audios = data.value.audios.minusElement(
                                    audioPair?.first ?: return@MediumAudioPlayer
                                )
                            )
                            scope.launch {
                                snackBarHostState.currentSnackbarData?.dismiss()
                                val result = snackBarHostState.showSnackbar(
                                    message = recordDeletedResource,
                                    actionLabel = undoResource,
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    data.value = data.value.copy(
                                        audios = data.value.audios.plusElement(audioPair.first)
                                    )
                                }
                                deletedAudios.remove(audioPair)
                            }
                        }
                    }

                    remindersContext(
                        alarmModel,
                        snackbarState,
                        toolTipState,
                        data,
                        scope,
                        deletedReminders,
                        jobCounter
                    )
                }

                // --- CASCADE EDITOR SECTION (Takes remaining space) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CascadeEditor(
                        stateHolder = editorState,
                        textStates = textStates,
                        modifier = Modifier.fillMaxSize(),
                        theme = cascadeTheme
                    )

                    // Smart Note Placeholder Logic
                    val showPlaceholder by remember {
                        derivedStateOf {
                            val blocks = editorState.state.blocks
                            blocks.isEmpty() || (blocks.size == 1 && textStates.getVisibleText(blocks.first().id).isNullOrEmpty())
                        }
                    }

                    if (showPlaceholder) {
                        val horizontalPadding = cascadeTheme.dimensions.blockHorizontalPadding
                        Text(
                            text = "Note",
                            color = Color(textColorState.intValue).copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !isImeVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    keyboard?.hide()
                                    permissionsModel.requestPermission(Permission.RECORD_AUDIO).collect {
                                        when (it) {
                                            PermissionResult.Denied -> Toast.makeText(context, permissionDeniedResource, Toast.LENGTH_SHORT).show()
                                            is PermissionResult.Error -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                            PermissionResult.Granted -> showRecorderSheet.value = true
                                            PermissionResult.PermanentlyDenied -> permissionsModel.permissionDeniedHandle(snackBarHostState)
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                            contentColor = contentColorFor(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(painterResource(CommonIcons.MIC), null)
                        }

                        SmallFloatingActionButton(
                            onClick = {
                                scope.launch {
                                    permissionsModel.requestPermission(Permission.CAMERA).collect {
                                        when (it) {
                                            PermissionResult.Denied -> Toast.makeText(context, permissionDeniedResource, Toast.LENGTH_SHORT).show()
                                            is PermissionResult.Error -> Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                            PermissionResult.Granted -> cameraLauncher.launch(getPictureUri())
                                            PermissionResult.PermanentlyDenied -> permissionsModel.permissionDeniedHandle(snackBarHostState)
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                            contentColor = contentColorFor(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(painterResource(CommonIcons.CAMERA), null)
                        }

                        SmallFloatingActionButton(
                            onClick = {
                                dataModel.sendAction(
                                    Action.Insert(
                                        data.value.copy(
                                            uid = data.value.uid,
                                            title = untitledResource,
                                            blocks = editorState.state.blocks,
                                            background = backgroundColorState.intValue,
                                            textColor = textColorState.intValue
                                        )
                                    )
                                )
                                linkModel.urlsProcessing(data.value.uid, descriptionState.text.toString())
                                onDismiss()
                            },
                            shape = CircleShape,
                            containerColor = CommonColors.prime,
                            contentColor = MaterialTheme.colorScheme.surface
                        ) {
                            Icon(painterResource(CommonIcons.CHECK), null)
                        }
                    }
                }
            }

            Box(Modifier.fillMaxWidth()) {
                // Snackbar overlays content without needing a Scaffold slot
                SnackbarHost(
                    hostState = snackBarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 72.dp) // clears the action row height
                ) { snackbarData ->
                    Snackbar(
                        snackbarData = snackbarData,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = CommonColors.prime,
                    )
                }
            }
        }
    }

    // ── Recorder Bottom Sheet ─────────────────────────────────────────────────
    if (showRecorderSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showRecorderSheet.value = false },
            sheetState = recorderSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            RecorderSheet(
                data = data,
                onRecordingComplete = {
                    scope.launch {
                        recorderSheetState.hide()
                        showRecorderSheet.value = false
                    }
                }
            )
        }
    }

    // ── Reminder Bottom Sheet ─────────────────────────────────────────────────
    if (showReminderSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showReminderSheet.value = false },
            sheetState = reminderSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ReminderSheet(
                data = data,
                onReminderSet = {
                    scope.launch {
                        reminderSheetState.hide()
                        showReminderSheet.value = false
                    }
                }
            )
        }
    }
}

fun createMediaUri(context: Context, pattern: String): Uri {
    val (directory, extension) = when (pattern.lowercase()) {
        "jpg", "jpeg" -> Environment.DIRECTORY_PICTURES to "jpg"
        "mp4" -> Environment.DIRECTORY_MOVIES to "mp4"
        else -> throw IllegalArgumentException("Unsupported media type: $pattern")
    }

    val mediaDirectory = context.getExternalFilesDir(directory)
        ?: throw IllegalStateException("App-specific media directory is unavailable")

    if (!mediaDirectory.exists() && !mediaDirectory.mkdirs()) {
        throw IllegalStateException("Unable to create media directory")
    }

    val outputFile = File(mediaDirectory, "${Random.nextLong()}.$extension")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        outputFile
    )
}
