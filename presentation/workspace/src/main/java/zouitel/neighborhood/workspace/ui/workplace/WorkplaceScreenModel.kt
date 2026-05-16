package zouitel.neighborhood.workspace.ui.workplace

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.consume
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy.*
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.MediaType
import zouitel.neighborhood.workspace.ui.utils.getImageDisplayName
import zouitel.neighborhood.workspace.worker.CopyMediaWorker
import kotlinx.coroutines.launch
import zouitel.neighborhood.ui.getAverageColorFromImage
import java.io.File
import kotlin.random.Random

@Stable
class WorkplaceScreenModel(val application: Application) : ScreenModel {

    private val workManager = WorkManager.getInstance(application)

    fun handleContentReceiver(data: MutableState<Data>, content: TransferableContent) {
        content.consume {
            it.uri?.let { uri ->
                screenModelScope.launch {
                    val mime = application.contentResolver.getType(uri) ?: ""
                    val mediaType = when {
                        mime.startsWith("video/") -> MediaType.VIDEO
                        mime.startsWith("image/") -> MediaType.IMAGE
                        else -> MediaType.DOCUMENT
                    }

                    val mediaDir = when (mediaType) {
                        MediaType.DOCUMENT -> Environment.DIRECTORY_DOCUMENTS
                        MediaType.VIDEO -> Environment.DIRECTORY_MOVIES
                        MediaType.IMAGE -> Environment.DIRECTORY_PICTURES
                    }.let { dir -> application.getExternalFilesDir(dir) }

                    val mediaName = getImageDisplayName(application, uri)
                    val destinationPath = File(mediaDir, mediaName).path

                    val averageColor = when (mediaType) {
                        MediaType.IMAGE -> getAverageColorFromImage(application, uri.toString())
                        else -> Color.DarkGray
                    }

                    data.value = data.value.copy(
                        medias = data.value.medias.plusElement(
                            Media(
                                uri = uri.toString(),
                                type = mediaType,
                                color = averageColor.toArgb()
                            )
                        )
                    )

                    enqueueFileProcessing(uri, destinationPath) { copiedPath ->
                        data.value = data.value.copy(
                            medias = data.value.medias.map { media ->
                                if (media.uri == uri.toString()) media.copy(uri = copiedPath)
                                else media
                            }
                        )
                    }
                }
            } ?: return@consume false
            true
        }
    }

    fun enqueueFileProcessing(
        uri: Uri,
        destinationPath: String,
        onComplete: (String) -> Unit = {}
    ) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            "CONTENT_URI" to uri.toString(),
            "DESTINATION_PATH" to destinationPath
        )

        val request = OneTimeWorkRequestBuilder<CopyMediaWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "${WORK_NAME}${Random.nextInt()}",
            APPEND_OR_REPLACE,
            request
        )

        workManager.getWorkInfoByIdLiveData(request.id).observeForever { workInfo ->
            if (workInfo?.state?.isFinished == true) {
                onComplete(destinationPath)
            }
        }
    }

    companion object {
        const val WORK_NAME = "unique_workplace_work_"
    }
}