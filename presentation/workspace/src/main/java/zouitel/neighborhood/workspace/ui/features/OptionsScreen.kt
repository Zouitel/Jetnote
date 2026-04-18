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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import dev.icerock.moko.permissions.storage.STORAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zouitel.neighborhood.permissions.PermissionResult
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonIcons.BELL
import zouitel.neighborhood.ui.CommonIcons.CAMERA
import zouitel.neighborhood.ui.CommonIcons.DOCUMENT
import zouitel.neighborhood.ui.CommonIcons.GALLERY
import zouitel.neighborhood.ui.CommonIcons.LIST_CHECK
import zouitel.neighborhood.ui.CommonIcons.MIC
import zouitel.neighborhood.ui.CommonIcons.TAGS
import zouitel.neighborhood.ui.CommonIcons.VIDEO_CAMERA
import zouitel.neighborhood.ui.CommonIcons.WAVEFORM
import zouitel.neighborhood.ui.CommonOptionItem
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.SoundEffect
import zouitel.neighborhood.ui.getAverageColorFromImage
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.MediaType
import zouitel.neighborhood.workspace.model.Priorities
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.workspace.ui.utils.NoteDates
import zouitel.neighborhood.workspace.ui.utils.PrioritySlider
import zouitel.neighborhood.workspace.ui.utils.getImageDisplayName
import zouitel.neighborhood.workspace.ui.workplace.WorkplaceScreenModel
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

data class OptionsScreen(
    val data: MutableState<Data>,
    val titleState: TextFieldState?,
    val descriptionState: TextFieldState?
) : Screen {

    @Composable
    override fun Content() {
        Options(
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
        getScreenModel()
        )
    }

    @Composable
    private fun Options(
        settingsModel   : SettingsScreenModel,
        workspaceModel  : WorkplaceScreenModel,
        permissionsModel: PermissionScreenModel,
        OCRModel: OCRScreenModel
    ) {
        val context   = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val bottomNav = LocalBottomSheetNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val tagsScreen       = rememberScreen(SharedScreen.Tags(data.value.uid, data.value.background))
        val scope            = rememberCoroutineScope()
        val pictureUri       = rememberSaveable { mutableStateOf<Uri?>(null) }
        val videoUri         = rememberSaveable { mutableStateOf<Uri?>(null) }
        val snackBarHostState  = remember { SnackbarHostState() }
        val recorderSheetState = rememberModalBottomSheetState()
        val showRecorderSheet  = remember { mutableStateOf(false) }
        val reminderSheetState = rememberModalBottomSheetState(true)
        val showReminderSheet  = remember { mutableStateOf(false) }
        val sound by lazy { SoundEffect() }

        // ── Document picker ───────────────────────────────────────────────────
        val documentLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { sourceUri: Uri? ->
            if (sourceUri == null) { bottomNav.hide(); return@rememberLauncherForActivityResult }

            scope.launch {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                // ── Reliable extension detection ──────────────────────────────
                val displayName = resolveDisplayName(context, sourceUri)
                val ext = run {
                    val extFromName = displayName.substringAfterLast('.', "").lowercase()
                    extFromName.ifBlank {
                        extensionFromMime(
                            context.contentResolver.getType(sourceUri) ?: "application/pdf"
                        )
                    }
                }
                // ─────────────────────────────────────────────────────────────

                val destUri = createMediaUri(context, ext)

                val destPath = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(sourceUri)?.use { src ->
                            context.contentResolver.openOutputStream(destUri)?.use { dst ->
                                src.copyTo(dst)
                            }
                        }
                        filePathFromUri(context, destUri)
                    }.getOrNull()
                }

                if (destPath == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to save document.", Toast.LENGTH_SHORT)
                            .show()
                    }
                    bottomNav.hide()
                    return@launch
                }

                // Save to database — type = DOCUMENT, no auto-open
                data.value = data.value.copy(
                    medias = data.value.medias.plusElement(
                        Media(
                            uri = destPath,
                            type = MediaType.DOCUMENT,
                            color = Color.Transparent.toArgb()
                        )
                    )
                )

                // ✦ Do NOT open DocViewer here — user taps the thumbnail to open it
                bottomNav.hide()
            }
        }
        // ─────────────────────────────────────────────────────────────────────

        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            uris.fastForEachIndexed { _, uri ->
                scope.launch {
                    val mediaName    = getImageDisplayName(context, uri)
                    val mime         = context.contentResolver.getType(uri) ?: ""
                    val mediaType    = if (mime.startsWith("video/")) MediaType.VIDEO else MediaType.IMAGE
                    val mediaDir     = if (mediaType == MediaType.VIDEO) {
                        Environment.DIRECTORY_MOVIES?.let { context.getExternalFilesDir(it) }
                    } else {
                        Environment.DIRECTORY_PICTURES?.let { context.getExternalFilesDir(it) }
                    }
                    val destPath     = File(mediaDir, mediaName).path
                    val averageColor = if (mediaType == MediaType.IMAGE) {
                        getAverageColorFromImage(context, uri.toString())
                    } else Color.DarkGray

                    data.value = data.value.copy(
                        medias = data.value.medias.plusElement(
                            Media(uri = uri.toString(), type = mediaType,
                                color = averageColor.toArgb())
                        )
                    )

                    workspaceModel.enqueueFileProcessing(uri, destPath) { copiedPath ->
                        data.value = data.value.copy(
                            medias = data.value.medias.map { m ->
                                if (m.uri == uri.toString()) m.copy(uri = copiedPath) else m
                            }
                        )
                    }
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
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
                    val mediaDir   = Environment.DIRECTORY_PICTURES
                        ?.let { context.getExternalFilesDir(it) }
                    val mediaName  = getImageDisplayName(context, pictureUri.value ?: return@launch)
                    val destPath   = File(mediaDir, mediaName).path
                    val avgColor   = getAverageColorFromImage(context, pictureUri.value.toString())
                    data.value = data.value.copy(
                        medias = data.value.medias.plusElement(
                            Media(uri = destPath, type = MediaType.IMAGE, color = avgColor.toArgb())
                        )
                    )
                }.invokeOnCompletion { bottomNav.hide() }
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
                            val mediaDir  = Environment.DIRECTORY_MOVIES
                                ?.let { context.getExternalFilesDir(it) }
                            val destFile  = File(mediaDir, "${System.currentTimeMillis()}.mp4")
                            context.contentResolver.openInputStream(resultUri)?.use { src ->
                                destFile.outputStream().use { dst -> src.copyTo(dst) }
                            }
                            data.value = data.value.copy(
                                medias = data.value.medias.plusElement(
                                    Media(uri = destFile.absolutePath, type = MediaType.VIDEO,
                                        color = Color.DarkGray.toArgb())
                                )
                            )
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error saving video: ${e.message}",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }.invokeOnCompletion { bottomNav.hide() }
            } else bottomNav.hide()
        }

        val getPictureUri: () -> Uri = remember {
            { val uri = createMediaUri(context, "jpg"); pictureUri.value = uri; uri }
        }

        SideEffect {
            (context as Activity).window?.navigationBarColor =
                Color(data.value.background.let { Color.Black.copy(alpha = .7f).toArgb() }).toArgb()
        }

        CommonBottomSheet(data.value.background) {
            LazyColumn(modifier = Modifier.navigationBarsPadding()) {

                item {
                    CommonOptionItem(name = "Gallery", icon = GALLERY) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            galleryLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        }
                    }
                }

                item {
                    CommonOptionItem(name = "Add Audio", icon = WAVEFORM) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            permissionsModel.requestPermission(Permission.STORAGE).collect {
                                when (it) {
                                    PermissionResult.Denied -> Toast.makeText(
                                        context, "Permission is Denied!", Toast.LENGTH_SHORT).show()
                                    is PermissionResult.Error -> Toast.makeText(
                                        context, it.message, Toast.LENGTH_SHORT).show()
                                    else -> navigator.push(AudioListScreen(data))
                                }
                            }
                        }
                    }
                }

                item {
                    CommonOptionItem(name = "Take Photo", icon = CAMERA) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            permissionsModel.requestPermission(Permission.CAMERA).collect {
                                when (it) {
                                    PermissionResult.Denied -> Toast.makeText(
                                        context, "Permission is Denied!", Toast.LENGTH_SHORT).show()
                                    is PermissionResult.Error -> Toast.makeText(
                                        context, it.message, Toast.LENGTH_SHORT).show()
                                    PermissionResult.Granted ->
                                        cameraLauncher.launch(getPictureUri())
                                    PermissionResult.PermanentlyDenied ->
                                        permissionsModel.permissionDeniedHandle(snackBarHostState)
                                }
                            }
                        }
                    }
                }

                item {
                    CommonOptionItem(name = "Video Capture", icon = VIDEO_CAMERA) {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            permissionsModel.requestPermission(Permission.CAMERA).collect {
                                when (it) {
                                    PermissionResult.Denied -> Toast.makeText(
                                        context, "Permission is Denied!", Toast.LENGTH_SHORT).show()
                                    is PermissionResult.Error -> Toast.makeText(
                                        context, it.message, Toast.LENGTH_SHORT).show()
                                    PermissionResult.Granted -> videoCapture.launch(
                                        Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                                            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
                                        }
                                    )
                                    PermissionResult.PermanentlyDenied ->
                                        permissionsModel.permissionDeniedHandle(snackBarHostState)
                                }
                            }
                        }
                    }
                }

                item {
                    CommonOptionItem(name = "Recorder", icon = MIC) {
                        scope.launch {
                            permissionsModel.requestPermission(Permission.RECORD_AUDIO)
                                .collectLatest {
                                    when (it) {
                                        PermissionResult.Denied -> Toast.makeText(
                                            context, "Permission is Denied!", Toast.LENGTH_SHORT).show()
                                        is PermissionResult.Error -> Toast.makeText(
                                            context, it.message, Toast.LENGTH_SHORT).show()
                                        PermissionResult.Granted -> showRecorderSheet.value = true
                                        PermissionResult.PermanentlyDenied ->
                                            permissionsModel.permissionDeniedHandle(snackBarHostState)
                                    }
                                }
                        }
                    }
                }

                item {
                    CommonOptionItem(name = "Document", icon = DOCUMENT) {
                        sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                        documentLauncher.launch(
                            arrayOf(
                                "application/pdf",
                                "application/msword",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "application/vnd.ms-excel",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-powerpoint",
                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                "text/plain"
                            )
                        )
                    }
                }

//                item {
//                    CommonOptionItem(name = "Scanner", icon = DOCUMENT) {
//                        navigator.push(OCRScreen(data.value.uid))
//                    }
//                }

                item {
                    CommonOptionItem(name = "Tags", icon = TAGS) {
                        sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                        bottomNav.show(tagsScreen)
                    }
                }

                item {
                    CommonOptionItem(name = "Task List", icon = LIST_CHECK) {
                        sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                        bottomNav.show(AddTaskScreen(null, data))
                    }
                }

                item {
                    CommonOptionItem(name = "Reminder", icon = BELL) {
                        scope.launch {
                            permissionsModel.requestPermission(Permission.REMOTE_NOTIFICATION)
                                .collect {
                                    when (it) {
                                        PermissionResult.Denied -> Toast.makeText(
                                            context, "Permission is Denied!", Toast.LENGTH_SHORT).show()
                                        is PermissionResult.Error -> Toast.makeText(
                                            context, it.message, Toast.LENGTH_SHORT).show()
                                        PermissionResult.Granted -> showReminderSheet.value = true
                                        PermissionResult.PermanentlyDenied ->
                                            permissionsModel.permissionDeniedHandle(snackBarHostState)
                                    }
                                }
                        }
                    }
                }

                item {
                    val currentPriority = Priorities.entries[data.value.priority]
                    val priorityPair    = getPriority(data.value.priority)
                    Text(
                        text     = if (currentPriority == Priorities.NON) "Priority"
                        else "Priority · ${currentPriority.priority}",
                        modifier = Modifier.padding(20.dp, 15.dp),
                        style    = MaterialTheme.typography.labelLarge,
                        color    = if (currentPriority == Priorities.NON)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
                        else Color(priorityPair.second).copy(alpha = .9f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    PrioritySlider(
                        priority         = currentPriority,
                        onPriorityChange = { priority ->
                            data.value = data.value.copy(priority = priority.index)
                        }
                    )
                }

                item { NoteDates(data.value) }

                item {
                    SnackbarHost(snackBarHostState) {
                        Snackbar(
                            snackbarData   = it,
                            modifier       = Modifier.animateContentSize(),
                            shape          = MaterialTheme.shapes.large,
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor   = MaterialTheme.colorScheme.surface,
                            actionColor    = MaterialTheme.colorScheme.surface
                        )
                    }
                }
            }

            if (showRecorderSheet.value) {
                ModalBottomSheet(
                    onDismissRequest = { showRecorderSheet.value = false },
                    sheetState       = recorderSheetState,
                    containerColor   = MaterialTheme.colorScheme.surface
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

            if (showReminderSheet.value) {
                ModalBottomSheet(
                    onDismissRequest = { showReminderSheet.value = false },
                    sheetState       = reminderSheetState,
                    containerColor   = MaterialTheme.colorScheme.surface
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressLint("SimpleDateFormat")
    private fun createMediaUri(context: Context, pattern: String): Uri {
        val type = MediaType.fromExtension(pattern)

        val dir = when (type) {
            MediaType.DOCUMENT -> Environment.DIRECTORY_DOCUMENTS
            MediaType.VIDEO    -> Environment.DIRECTORY_MOVIES
            MediaType.IMAGE    -> Environment.DIRECTORY_PICTURES
        }.let { context.getExternalFilesDir(it) }

        if (!dir?.exists()!!) dir.mkdirs()

        val fileName = if (type == MediaType.DOCUMENT) "doc_${System.currentTimeMillis()}.$pattern"
        else "${Random.nextLong()}.$pattern"

        val newFile = File(dir, fileName)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", newFile)
    }

    private fun filePathFromUri(context: Context, uri: Uri): String? =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name    = cursor.getString(idx)
                        val docsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        File(docsDir, name).takeIf { it.exists() }?.absolutePath
                    } else null
                } else null
            }
        }.getOrNull()
            ?: uri.path?.substringAfter("/external_files")
                ?.let { Environment.getExternalStorageDirectory().absolutePath + it }

    private fun resolveDisplayName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull() ?: "document_${System.currentTimeMillis()}"

    /**
     * Extension from MIME — used only when the display-name has no extension.
     * Explicit MIME checks come before any "contains" checks to avoid
     * xlsx/pptx matching "text" on devices that return ambiguous types.
     */
    private fun extensionFromMime(mime: String): String = when (mime) {
        "application/pdf" -> "pdf"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        "application/vnd.ms-excel" -> "xls"
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
        "application/vnd.ms-powerpoint" -> "ppt"
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
        "text/plain" -> "txt"
        else -> when {
            mime.startsWith("video/") -> "mp4"
            mime.startsWith("image/") -> "jpg"
            else -> "pdf"      // safe fallback
        }
    }

    fun getPriority(priority: Int) = when (priority) {
        1    -> Pair("Low",    0xff32cd32)
        2    -> Pair("Medium", 0xffffef00)
        3    -> Pair("High",   0xffffa500)
        4    -> Pair("Urgent", 0xffff2800)
        else -> Pair("Non",    0xffc0c0c0)
    }
}