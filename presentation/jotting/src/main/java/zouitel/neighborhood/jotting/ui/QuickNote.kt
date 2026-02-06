package zouitel.neighborhood.jotting.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.FileProvider
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.audio.utils.MediumAudioPlayer
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.permissions.PermissionResult
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.jotting.model.Screens
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import zouitel.neighborhood.workspace.ui.features.RecorderSheet
import zouitel.neighborhood.workspace.ui.features.ReminderSheet
import zouitel.neighborhood.workspace.ui.features.getAverageColorFromImage
import zouitel.neighborhood.workspace.ui.utils.remindersContext
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreenModel
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNote(
    screen: MutableState<Screens?>,
    data: MutableState<Data>,
    onDismiss: () -> Unit
) {
    val playerModel = koinInject<AudioScreenModel>()
    val alarmModel =  koinInject<AlarmManagerScreenModel>()
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

    // Bottom sheet states
    val showRecorderSheet = remember { mutableStateOf(false) }
    val showReminderSheet = remember { mutableStateOf(false) }
    val recorderSheetState = rememberModalBottomSheetState()
    val reminderSheetState = rememberModalBottomSheetState()

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

    val descriptionState = rememberTextFieldState(data.value.description)
    val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
    val backgroundColorState = rememberSaveable { mutableIntStateOf(backgroundColor) }
    val textColor = contentColorFor(MaterialTheme.colorScheme.surface).toArgb()
    val textColorState = rememberSaveable { mutableIntStateOf(textColor) }

    // Replace your existing LaunchedEffect(screen.value) for focus with this:
    LaunchedEffect(Unit) { // Use Unit or a specific "Open" trigger
        // 1. Wait for the composition to settle and the window to be ready
        kotlinx.coroutines.delay(200)

        // 2. Request focus on the TextField
        focus.requestFocus()

        // 3. Explicitly tell the keyboard controller to show
        // Note: Sometimes keyboard?.show() fails if not called directly on the focusable view
        keyboard?.show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pictureUri.value != null) {
            scope.launch {
                val averageColor = getAverageColorFromImage(context, pictureUri.value.toString())
                data.value = data.value.copy(
                    medias = data.value.medias.plusElement(Media(uri = pictureUri.value.toString(), color = averageColor.toArgb()))
                )
            }
        }
    }

    LaunchedEffect(screen.value) {
        dataModel.sendAction(Action.Insert(data))
        when (screen.value) {
            Screens.NOTE_SCREEN -> {
                playerModel.run {
                    initController()
                }
            }
            Screens.RECORD_SCREEN -> {
                permissionsModel.requestPermission(Permission.RECORD_AUDIO)
                    .collect {
                        when(it) {
                            PermissionResult.Denied -> {
                                Toast.makeText(
                                    context,
                                    "Permission is Denied!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is PermissionResult.Error -> {
                                Toast.makeText(
                                    context,
                                    it.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            PermissionResult.Granted -> {
                                showRecorderSheet.value = true
                            }
                            PermissionResult.PermanentlyDenied -> {
                                permissionsModel.permissionDeniedHandle(snackBarHostState)
                            }
                        }
                    }
            }
            Screens.PHOTO_SCREEN -> {
                scope.launch {
                    permissionsModel.requestPermission(Permission.CAMERA).collect {
                        when(it) {
                            PermissionResult.Denied -> {
                                Toast.makeText(
                                    context,
                                    "Permission is Denied!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            is PermissionResult.Error -> {
                                Toast.makeText(
                                    context,
                                    it.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            PermissionResult.Granted -> cameraLauncher.launch(getPictureUri())
                            PermissionResult.PermanentlyDenied -> {
                                permissionsModel.permissionDeniedHandle(snackBarHostState)
                            }
                        }
                    }
                }
            }

            Screens.REMINDER_SCREEN -> {
                showReminderSheet.value = true
            }
            null -> Unit
        }
        screen.value = null
    }

    // Custom Popup that looks like a tooltip but supports interaction
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false  // Allow custom width
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(snackBarHostState) { data ->
                    Snackbar(
                        modifier = Modifier.imePadding(),
                        snackbarData = data,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = CommonColors.prime,
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(270.dp)
                        .padding(start = 25.dp, end = 25.dp)
                        .imePadding()
                        .background(Color.Transparent),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column {
                        // media display.
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
                                    message = "media deleted",
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
                                mediaPair.let { deletedMedias.remove(it) }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .background(Color.Transparent)
                                .fillMaxWidth()
                                .heightIn(300.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // description field.
                            item {
                                CommonTextField(
                                    state = descriptionState,
                                    modifier = Modifier.focusRequester(focus),
                                    placeholder = "Note",
                                    textColor = MaterialTheme.colorScheme.onSurface
                                ) {
                                    workplaceModel.handleContentReceiver(data, it)
                                }
                            }

                            //audios display.
                            items(data.value.audios) { audio ->
                                MediumAudioPlayer(playerModel, audio) {
                                    val currentJobId = jobCounter.intValue++
                                    deletedAudios.add(Pair(audio, currentJobId))
                                    val audioPair = deletedAudios.firstOrNull { it.second == currentJobId }

                                    data.value = data.value.copy(
                                        audios = data.value.audios.minusElement(audioPair?.first ?: return@MediumAudioPlayer)
                                    )

                                    scope.launch {
                                        snackBarHostState.currentSnackbarData?.dismiss()
                                        val result = snackBarHostState.showSnackbar(
                                            message = "record deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Long
                                        )

                                        if (result == SnackbarResult.ActionPerformed) {
                                            audioPair.first.let { audio ->
                                                data.value = data.value.copy(
                                                    audios = data.value.audios.plusElement(audio)
                                                )
                                            }
                                        }
                                        audioPair.let { deletedAudios.remove(it) }
                                    }
                                }
                            }

                            // display reminder chips.
                            remindersContext(
                                alarmModel,
                                snackbarState,
                                toolTipState,
                                data,
                                scope,
                                deletedReminders,
                                jobCounter
                            )

                            //bottom options.
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    SmallFloatingActionButton(
                                        onClick = onDismiss,
                                        shape = CircleShape,
                                        containerColor = MaterialTheme.colorScheme.outlineVariant,
                                        contentColor = contentColorFor(
                                            backgroundColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    ) {
                                        Icon(painterResource(CommonIcons.CROSS_SMALL), null)
                                    }
                                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                        SmallFloatingActionButton(
                                            onClick = {
                                                scope.launch {
                                                    keyboard?.hide()
                                                    permissionsModel.requestPermission(Permission.RECORD_AUDIO)
                                                        .collect {
                                                            when(it) {
                                                                PermissionResult.Denied -> {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Permission is Denied!",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                                is PermissionResult.Error -> {
                                                                    Toast.makeText(
                                                                        context,
                                                                        it.message,
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                                PermissionResult.Granted -> {
                                                                    showRecorderSheet.value = true
                                                                }
                                                                PermissionResult.PermanentlyDenied -> {
                                                                    permissionsModel.permissionDeniedHandle(snackBarHostState)
                                                                }
                                                            }
                                                        }
                                                }
                                            },
                                            shape = CircleShape,
                                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                                            contentColor = contentColorFor(
                                                backgroundColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Icon(painterResource(CommonIcons.MIC), null)
                                        }

                                        SmallFloatingActionButton(
                                            onClick = {
                                                scope.launch {
                                                    permissionsModel.requestPermission(Permission.CAMERA)
                                                        .collect {
                                                            when(it) {
                                                                PermissionResult.Denied -> {
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Permission is Denied!",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                                is PermissionResult.Error -> {
                                                                    Toast.makeText(
                                                                        context,
                                                                        it.message,
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                                PermissionResult.Granted -> cameraLauncher.launch(getPictureUri())
                                                                PermissionResult.PermanentlyDenied -> {
                                                                    permissionsModel.permissionDeniedHandle(snackBarHostState)
                                                                }
                                                            }
                                                        }
                                                }
                                            },
                                            shape = CircleShape,
                                            containerColor = MaterialTheme.colorScheme.outlineVariant,
                                            contentColor = contentColorFor(
                                                backgroundColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Icon(painterResource(CommonIcons.CAMERA), null)
                                        }

                                        SmallFloatingActionButton(
                                            onClick = {
                                                dataModel.sendAction(
                                                    Action.Insert(
                                                        data.value.copy(
                                                            uid = data.value.uid,
                                                            title = "Untitled",
                                                            description = descriptionState.text.toString(),
                                                            background = backgroundColorState.intValue,
                                                            textColor = textColorState.intValue
                                                        )
                                                    )
                                                )
                                                // links handel.
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
                        }
                    }
                }
            }
        }
    }

// RecorderSheet Bottom Sheet
    if (showRecorderSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                showRecorderSheet.value = false
            },
            sheetState = recorderSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            RecorderSheet(
                data = data,
                onRecordingComplete = {
                    // Auto-close the bottom sheet when recording is complete
                    scope.launch {
                        recorderSheetState.hide()
                        showRecorderSheet.value = false
                    }
                }
            )
        }
    }

// Reminder Bottom Sheet
    if (showReminderSheet.value) {
        ModalBottomSheet(
            onDismissRequest = {
                showReminderSheet.value = false
            },
            sheetState = reminderSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ReminderSheet(
                data = data,
                onReminderSet = {
                    // Auto-close the bottom sheet when reminder is set
                    scope.launch {
                        reminderSheetState.hide()
                        showReminderSheet.value = false
                    }
                }
            )
        }
    }
}

@SuppressLint("SimpleDateFormat")
fun createMediaUri(context: Context, pattern: String): Uri {
    val fileName = "${Random.nextLong()}.$pattern"
    val imagePath = when(pattern) {
        "jpg" -> Environment.DIRECTORY_PICTURES
        "mp4" -> Environment.DIRECTORY_MOVIES
        else -> Environment.DIRECTORY_DOWNLOADS
    }?.let { context.getExternalFilesDir(it) }
    if (!imagePath?.exists()!!) imagePath.mkdirs()
    val newFile = File(imagePath, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", newFile)
}