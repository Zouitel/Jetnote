package zouitel.neighborhood.workspace.ui.features

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import zouitel.neighborhood.permissions.PermissionResult
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonIcons.BELL
import zouitel.neighborhood.ui.CommonIcons.CAMERA
import zouitel.neighborhood.ui.CommonIcons.GALLERY
import zouitel.neighborhood.ui.CommonIcons.LIST_CHECK
import zouitel.neighborhood.ui.CommonIcons.MIC
import zouitel.neighborhood.ui.CommonIcons.TAGS
import zouitel.neighborhood.ui.CommonIcons.VIDEO_CAMERA
import zouitel.neighborhood.ui.CommonIcons.WAVEFORM
import zouitel.neighborhood.ui.CommonOptionItem
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.SoundEffect
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.Priorities
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.workspace.ui.utils.getImageDisplayName
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreenModel
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import dev.icerock.moko.permissions.storage.STORAGE
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

data class OptionsScreen(
    val data: MutableState<Data>,
    val titleState: TextFieldState?,
    val descriptionState: TextFieldState?
): Screen {
    @Composable
    override fun Content() {
        Options(
            settingsModel = getScreenModel(),
            workspaceModel = getScreenModel(),
            permissionsModel = getScreenModel()
        )
    }

    @Composable
    private fun Options(
        settingsModel: SettingsScreenModel,
        workspaceModel: WorkplaceScreenModel,
        permissionsModel: PermissionScreenModel
    ) {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val bottomNav = LocalBottomSheetNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val tagsScreen = rememberScreen(SharedScreen.Tags(data.value.uid, data.value.background))
        val scope = rememberCoroutineScope()
        val pictureUri = rememberSaveable { mutableStateOf<Uri?>(null) }
        val videoUri = rememberSaveable { mutableStateOf<Uri?>(null) }
        val snackBarHostState = remember { SnackbarHostState() }
        val recorderSheetState = rememberModalBottomSheetState()
        val showRecorderSheet = remember { mutableStateOf(false) }
        val reminderSheetState = rememberModalBottomSheetState(true)
        val showReminderSheet = remember { mutableStateOf(false) }
        val sound by lazy { SoundEffect() }

        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            uris.fastForEachIndexed { index, uri ->
                scope.launch {
                    val mediaName = getImageDisplayName(context, uri)
                    val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true

                    val mediaPath = if (isVideo) {
                        Environment.DIRECTORY_MOVIES?.let { context.getExternalFilesDir(it) }
                    } else {
                        Environment.DIRECTORY_PICTURES?.let { context.getExternalFilesDir(it) }
                    }

                    val destinationPath = File(mediaPath, mediaName).path
                    val averageColor = if (!isVideo) {
                        getAverageColorFromImage(context, uri.toString())
                    } else {
                        Color.DarkGray
                    }

                    // Add media with ORIGINAL URI immediately
                    data.value = data.value.copy(
                        medias = data.value.medias.plusElement(
                            Media(
                                uri = uri.toString(), // Use original URI
                                isVideo = isVideo,
                                color = averageColor.toArgb()
                            )
                        )
                    )

                    // Enqueue worker to copy file in background
                    workspaceModel.enqueueFileProcessing(uri, destinationPath) { copiedPath ->
                        // Update to local path after copy completes
                        val updatedMedias = data.value.medias.map { media ->
                            if (media.uri == uri.toString()) {
                                media.copy(uri = copiedPath)
                            } else {
                                media
                            }
                        }
                        data.value = data.value.copy(medias = updatedMedias)
                    }

                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            bottomNav.hide()
        }

        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && pictureUri.value != null) {
                scope.launch {
                    val mediaPath = Environment.DIRECTORY_PICTURES?.let { context.getExternalFilesDir(it) }
                    val mediaName = getImageDisplayName(context, pictureUri.value ?: return@launch)
                    val destinationPath = File(mediaPath, mediaName).path
                    val averageColor =
                        getAverageColorFromImage(context, pictureUri.value.toString())

                    data.value = data.value.copy(
                        medias = data.value.medias.plusElement(
                            Media(
                                uri = destinationPath,
                                color = averageColor.toArgb()
                            )
                        )
                    )
                }.invokeOnCompletion {
                    bottomNav.hide()
                }
            }
        }

        val videoCapture = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                scope.launch {
                    val resultUri = result.data?.data ?: videoUri.value

                    if (resultUri != null) {
                        try {
                            // Copy video to our desired location
                            val mediaPath = Environment.DIRECTORY_MOVIES?.let { context.getExternalFilesDir(it) }
                            val fileName = "${System.currentTimeMillis()}.mp4"
                            val destinationFile = File(mediaPath, fileName)

                            context.contentResolver.openInputStream(resultUri)?.use { input ->
                                destinationFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }

                            data.value = data.value.copy(
                                medias = data.value.medias.plusElement(
                                    Media(
                                        uri = destinationFile.absolutePath,
                                        isVideo = true,
                                        color = Color.DarkGray.toArgb()
                                    )
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving video: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.invokeOnCompletion {
                    bottomNav.hide()
                }
            } else {
                bottomNav.hide()
            }
        }

        val getPictureUri: () -> Uri = remember {
            {
                val uri = createMediaUri(context, "jpg")
                pictureUri.value = uri
                uri
            }
        }

        val getVideoUri: () -> Uri = remember {
            {
                val fileName = "${Random.nextLong()}.mp4"
                val mediaPath = Environment.DIRECTORY_MOVIES?.let { context.getExternalFilesDir(it) }
                if (!mediaPath?.exists()!!) mediaPath.mkdirs()
                val newFile = File(mediaPath, fileName)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", newFile)
                videoUri.value = uri
                uri
            }
        }

        SideEffect {
            (context as Activity).window?.navigationBarColor = Color(data.value.background.let { Color.Black.copy(alpha = .7f).toArgb() }).toArgb()
        }

        CommonBottomSheet(data.value.background) {
            LazyColumn(modifier = Modifier.navigationBarsPadding()) {
                item {
                    CommonOptionItem(
                        name = "Gallery",
                        icon = GALLERY
                    ) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    }
                }
                item {
                    CommonOptionItem(
                        name = "Add Audio",
                        icon = WAVEFORM
                    ) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            permissionsModel.requestPermission(Permission.STORAGE).collect {
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
                                    else -> navigator.push(AudioListScreen(data))
                                }
                            }
                        }
                    }
                }
                item {
                    CommonOptionItem(
                        name = "Take Photo",
                        icon = CAMERA
                    ) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
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
                }
                item {
                    CommonOptionItem(
                        name = "Video Capture",
                        icon = VIDEO_CAMERA
                    ) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
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
                                    PermissionResult.Granted -> {
                                        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                                            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                                            // Don't use EXTRA_OUTPUT - let camera app handle it
                                        }
                                        videoCapture.launch(intent)
                                    }
                                    PermissionResult.PermanentlyDenied -> {
                                        permissionsModel.permissionDeniedHandle(snackBarHostState)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    CommonOptionItem(
                        name = "Recorder",
                        icon = MIC
                    ) {
                        scope.launch {
                            permissionsModel.requestPermission(Permission.RECORD_AUDIO).collectLatest {
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
                    }
                }
                item {
                    CommonOptionItem(
                        name = "Tags",
                        icon = TAGS
                    ) {
                        sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                        bottomNav.show(tagsScreen)
                    }
                }
                item {
                    CommonOptionItem(
                        name = "Task List",
                        icon = LIST_CHECK
                    ) {
                        sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                        bottomNav.show(AddTaskScreen(null, data))
                    }
                }
                item {
                    CommonOptionItem(
                        name = "Reminder",
                        icon = BELL
                    ) {
                        scope.launch {
                            permissionsModel.requestPermission(Permission.REMOTE_NOTIFICATION)
                                .collect {
                                    when (it) {
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
                                            showReminderSheet.value = true
                                        }
                                        PermissionResult.PermanentlyDenied -> {
                                            permissionsModel.permissionDeniedHandle(snackBarHostState)
                                        }
                                    }
                                }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.width(16.dp))

                    PrioritySlider(
                        priority = Priorities.entries[data.value.priority],
                        onPriorityChange = { priority ->
                            data.value = data.value.copy(priority = priority.index)
                        }
                    )
                }
                item {
                    SnackbarHost(snackBarHostState) {
                        Snackbar(
                            snackbarData = it,
                            modifier = Modifier.animateContentSize(),
                            shape = MaterialTheme.shapes.large,
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                            actionColor = MaterialTheme.colorScheme.surface,
                        )
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
                            scope.launch {
                                reminderSheetState.hide()
                                showReminderSheet.value = false
                            }
                        }
                    )
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun createMediaUri(context: Context, pattern: String): Uri {
        val fileName = "${Random.nextLong()}.$pattern"
        val mediaPath = when(pattern) {
            "jpg" -> Environment.DIRECTORY_PICTURES
            "mp4" -> Environment.DIRECTORY_MOVIES
            else -> Environment.DIRECTORY_DOWNLOADS
        }?.let { context.getExternalFilesDir(it) }
        if (!mediaPath?.exists()!!) mediaPath.mkdirs()
        val newFile = File(mediaPath, fileName)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", newFile)
    }

    @Composable
    private fun PrioritySlider(
        priority: Priorities,
        onPriorityChange: (Priorities) -> Unit
    ) {
        var sliderPosition by remember { mutableFloatStateOf(priority.ordinal.toFloat()) }
        var selectedPriority by remember { mutableStateOf(priority) }
        var sliderWidth by remember { mutableStateOf(0f) }
        val thumbColor = selectedPriority.color

        LaunchedEffect(sliderPosition) {
            selectedPriority = when (sliderPosition.toInt()) {
                1 -> Priorities.LOW
                2 -> Priorities.MED
                3 -> Priorities.HIG
                4 -> Priorities.URG
                else -> Priorities.NON
            }
            onPriorityChange(selectedPriority)
        }

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
            },
            valueRange = 0f..4f,
            steps = 3,
            onValueChangeFinished = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp)
                .onGloballyPositioned { coordinates ->
                    sliderWidth = coordinates.size.width.toFloat()
                },
            colors = SliderDefaults.colors(
                thumbColor = thumbColor,
                activeTrackColor = thumbColor.copy(alpha = 0.4f),
                inactiveTrackColor = thumbColor.copy(alpha = 0.15f),
            )
        )
    }
}