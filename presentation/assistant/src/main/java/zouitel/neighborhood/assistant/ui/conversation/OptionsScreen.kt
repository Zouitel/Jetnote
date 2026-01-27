package zouitel.neighborhood.assistant.ui.conversation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.assistant.utils.createChatFilesByContents
import zouitel.neighborhood.assistant.utils.getFileMimeType
import zouitel.neighborhood.assistant.utils.getFileNameFromUri
import zouitel.neighborhood.logic.appTempFolder
import zouitel.neighborhood.permissions.PermissionResult
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.ui.CommonIcons
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.camera.CAMERA
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import kotlin.uuid.Uuid

@Composable
internal fun Options(provider: ProviderSetting?, inputState: ChatInputState) {
    val permissionModel = koinInject<PermissionScreenModel>()

    Column {
        FlowRow(Modifier.padding(horizontal = 8.dp)) {
            TakePicButton(permissionModel) {
                inputState.addImages(it)
            }

            ImagePickButton {
                inputState.addImages(it)
            }

            FilePickButton {
                inputState.addFiles(it)
            }

            if (provider != null && provider is ProviderSetting.Google) {
                VideoPickButton {
                    inputState.addVideos(it)
                }

                AudioPickButton {
                    inputState.addAudios(it)
                }
            }

        }
    }
}

    @Composable
    private fun ImagePickButton(onAddImages: (List<Uri>) -> Unit = {}) {
        val context = LocalContext.current

        val (_, launchCrop) = useCropLauncher(
            onCroppedImageReady = { croppedUri ->
                onAddImages(context.createChatFilesByContents(listOf(croppedUri)))
            }
        )

        val imagePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                Log.d("ImagePickButton", "Selected URIs: $selectedUris")
                // todo Check if we should skip crop based on settings
                if (false/*settings.displaySetting.skipCropImage*/) {
                    // Skip crop, directly add images
                    onAddImages(context.createChatFilesByContents(selectedUris))
                } else {
                    // Show crop interface
                    if (selectedUris.size == 1) {
                        // Single image - offer crop
                        launchCrop(selectedUris.first())
                    } else {
                        // Multiple images - no crop
                        onAddImages(context.createChatFilesByContents(selectedUris))
                    }
                }
            } else {
                Log.d("ImagePickButton", "No images selected")
            }
        }

        Item(
            "Pictures",
            {
                Icon(painterResource(CommonIcons.PICTURE_36), null)
            }
        )
        {
            imagePickerLauncher.launch("image/*")
        }
    }

    @Composable
    private fun Item(text: String, icon: @Composable RowScope.() -> Unit, onClick: () -> Unit) {
        Column(
            Modifier.padding(15.dp, 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MediumExtendedFloatingActionButton(
                onClick = onClick,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                content = icon
            )
            Text(text)
        }
    }

    @Composable
    private fun useCropLauncher(
        onCroppedImageReady: (Uri) -> Unit,
        onCleanup: (() -> Unit)? = null
    ): Pair<ActivityResultLauncher<Intent>, (Uri) -> Unit> {
        val context = LocalContext.current
        var cropOutputUri by remember { mutableStateOf<Uri?>(null) }

        val cropActivityLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                cropOutputUri?.let { croppedUri ->
                    onCroppedImageReady(croppedUri)
                }
            }
            // Clean up crop output file
            cropOutputUri?.toFile()?.delete()
            cropOutputUri = null
            onCleanup?.invoke()
        }

        val launchCrop: (Uri) -> Unit = { sourceUri ->
            val outputFile = File(context.appTempFolder, "crop_output_${System.currentTimeMillis()}.jpg")
            cropOutputUri = Uri.fromFile(outputFile)

            val cropIntent = UCrop.of(sourceUri, cropOutputUri!!)
                .withOptions(UCrop.Options().apply {
                    setFreeStyleCropEnabled(true)
                    setAllowedGestures(
                        UCropActivity.SCALE,
                        UCropActivity.ROTATE,
                        UCropActivity.NONE
                    )
                    setCompressionFormat(Bitmap.CompressFormat.PNG)
                })
                .withMaxResultSize(4096, 4096)
                .getIntent(context)

            cropActivityLauncher.launch(cropIntent)
        }

        return Pair(cropActivityLauncher, launchCrop)
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @Composable
    private fun TakePicButton(permissionModel: PermissionScreenModel, onAddImages: (List<Uri>) -> Unit = {}) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
        var cameraOutputFile by remember { mutableStateOf<File?>(null) }

        val (_, launchCrop) = useCropLauncher(
            onCroppedImageReady = { croppedUri ->
                onAddImages(context.createChatFilesByContents(listOf(croppedUri)))
            },
            onCleanup = {
                // Clean up camera temp file after cropping is done
                cameraOutputFile?.delete()
                cameraOutputFile = null
                cameraOutputUri = null
            }
        )

        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { captureSuccessful ->
            if (captureSuccessful && cameraOutputUri != null) {
                // Check if we should skip crop based on settings
                if (false/*settings.displaySetting.skipCropImage*/) {
                    // Skip crop, directly add image
                    onAddImages(context.createChatFilesByContents(listOf(cameraOutputUri!!)))
                    // Clean up camera temp file
                    cameraOutputFile?.delete()
                    cameraOutputFile = null
                    cameraOutputUri = null
                } else {
                    // Show crop interface
                    launchCrop(cameraOutputUri!!)
                }
            } else {
                // Clean up camera temp file if capture failed
                cameraOutputFile?.delete()
                cameraOutputFile = null
                cameraOutputUri = null
            }
        }

        Item(
            "Camera",
            {
                Icon(painterResource(CommonIcons.CAMERA_36), null)
            }
        ) {
            scope.launch {
                permissionModel.requestPermission(Permission.CAMERA).collect {
                    when(it) {
                        PermissionResult.Denied -> TODO()
                        is PermissionResult.Error -> TODO()
                        PermissionResult.Granted -> {
                            // 权限已授权，直接启动相机
                            cameraOutputFile = context.cacheDir.resolve("camera_${Uuid.random()}.jpg")
                            cameraOutputUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                cameraOutputFile!!
                            )
                            cameraLauncher.launch(cameraOutputUri!!)
                        }
                        PermissionResult.PermanentlyDenied -> TODO()
                    }
                }
            }
        }
    }

    @Composable
    private fun VideoPickButton(onAddVideos: (List<Uri>) -> Unit = {}) {
        val context = LocalContext.current
        val videoPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                onAddVideos(context.createChatFilesByContents(selectedUris))
            }
        }

        Item(
            "Videos",
            {
                Icon(painterResource(CommonIcons.FILM_36), null)
            }
        ) {
            videoPickerLauncher.launch("video/*")
        }
    }

    @Composable
    private fun AudioPickButton(onAddAudios: (List<Uri>) -> Unit = {}) {
        val context = LocalContext.current
        val audioPickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                onAddAudios(context.createChatFilesByContents(selectedUris))
            }
        }

        Item(
            "Audios",
            {
                Icon(painterResource(CommonIcons.WAVEFORM_36), null)
            }
        ) {
            audioPickerLauncher.launch("audio/*")
        }
    }

    @Composable
    private fun FilePickButton(onAddFiles: (List<UIMessagePart.Document>) -> Unit = {}) {
        val context = LocalContext.current
        val pickMedia =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
                if (uris.isNotEmpty()) {
                    val allowedMimeTypes = setOf(
                        "text/plain",
                        "text/html",
                        "text/css",
                        "text/javascript",
                        "text/csv",
                        "text/xml",
                        "application/json",
                        "application/javascript",
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-powerpoint",
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    )

                    val documents = uris.mapNotNull { uri ->
                        val fileName = context.getFileNameFromUri(uri) ?: "file"
                        val mime = context.getFileMimeType(uri) ?: "text/plain"

                        // Filter by MIME type or file extension
                        val isAllowed = allowedMimeTypes.contains(mime) ||
                                mime.startsWith("text/") ||
                                mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                                mime == "application/pdf" ||
                                fileName.endsWith(".txt", ignoreCase = true) ||
                                fileName.endsWith(".md", ignoreCase = true) ||
                                fileName.endsWith(".csv", ignoreCase = true) ||
                                fileName.endsWith(".json", ignoreCase = true) ||
                                fileName.endsWith(".js", ignoreCase = true) ||
                                fileName.endsWith(".html", ignoreCase = true) ||
                                fileName.endsWith(".css", ignoreCase = true) ||
                                fileName.endsWith(".xml", ignoreCase = true) ||
                                fileName.endsWith(".py", ignoreCase = true) ||
                                fileName.endsWith(".java", ignoreCase = true) ||
                                fileName.endsWith(".kt", ignoreCase = true) ||
                                fileName.endsWith(".ts", ignoreCase = true) ||
                                fileName.endsWith(".tsx", ignoreCase = true) ||
                                fileName.endsWith(".md", ignoreCase = true) ||
                                fileName.endsWith(".markdown", ignoreCase = true) ||
                                fileName.endsWith(".mdx", ignoreCase = true) ||
                                fileName.endsWith(".yml", ignoreCase = true) ||
                                fileName.endsWith(".yaml", ignoreCase = true)

                        if (isAllowed) {
                            val localUri = context.createChatFilesByContents(listOf(uri))[0]
                            UIMessagePart.Document(
                                url = localUri.toString(),
                                fileName = fileName,
                                mime = mime
                            )
                        } else {
                            null
                        }
                    }

                    if (documents.isNotEmpty()) {
                        onAddFiles(documents)
                    } else {
                        // Show toast for unsupported file types
                    }
                }
            }

        Item(
            "Files",
            {
                Icon(painterResource(CommonIcons.CLIP_36), null)
            }
        ) {
            pickMedia.launch(arrayOf("*/*"))
        }
    }