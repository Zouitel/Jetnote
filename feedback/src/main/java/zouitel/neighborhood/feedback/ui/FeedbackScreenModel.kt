package zouitel.neighborhood.feedback.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import zouitel.neighborhood.feedback.worker.FeedbackUploadWorker
import zouitel.neighborhood.feedback.worker.FeedbackWorker
import java.util.UUID

class FeedbackScreenModel(application: Application) : ScreenModel {

    private val workManager = WorkManager.getInstance(application)

    // Drive upload tracking
    private val _uploadWorkId = MutableStateFlow<String?>(null)
    val uploadWorkId: StateFlow<String?> = _uploadWorkId.asStateFlow()

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    // ✅ NEW: GitHub issue submission tracking
    private val _submissionStatus = MutableStateFlow<SubmissionStatus>(SubmissionStatus.Idle)
    val submissionStatus: StateFlow<SubmissionStatus> = _submissionStatus.asStateFlow()

    init {
        // Observe Drive upload status
        screenModelScope.launch {
            _uploadWorkId.collect { workId ->
                workId?.let { id ->
                    observeUploadWork(id)
                }
            }
        }
    }

    /**
     * Upload screenshot to Google Drive
     */
    fun uploadScreenshotToDrive(screenshotUri: Uri) {
        screenModelScope.launch {
            try {
                Log.d("FeedbackScreenModel", "Starting screenshot upload to Drive: $screenshotUri")

                val fileName = "feedback_screenshot_${System.currentTimeMillis()}.png"

                val inputData = workDataOf(
                    FeedbackUploadWorker.Companion.KEY_SCREENSHOT_URI to screenshotUri.toString(),
                    FeedbackUploadWorker.Companion.KEY_SCREENSHOT_NAME to fileName
                )

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()

                val uploadRequest = OneTimeWorkRequestBuilder<FeedbackUploadWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .build()

                workManager.enqueue(uploadRequest)

                _uploadWorkId.value = uploadRequest.id.toString()
                _uploadStatus.value = UploadStatus.Uploading

                Log.d("FeedbackScreenModel", "Drive upload work enqueued: ${uploadRequest.id}")
            } catch (e: Exception) {
                Log.e("FeedbackScreenModel", "Error starting Drive upload", e)
                _uploadStatus.value = UploadStatus.Failed("Failed to start upload: ${e.message}")
            }
        }
    }

    /**
     * Observe the Drive upload work status
     */
    private fun observeUploadWork(workId: String) {
        screenModelScope.launch {
            try {
                val uuid = UUID.fromString(workId)

                workManager.getWorkInfoByIdFlow(uuid).collect { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.RUNNING -> {
                            _uploadStatus.value = UploadStatus.Uploading
                            Log.d("FeedbackScreenModel", "Drive upload running...")
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val driveLink = workInfo.outputData.getString(FeedbackUploadWorker.Companion.KEY_DRIVE_LINK)
                            val fileId = workInfo.outputData.getString(FeedbackUploadWorker.Companion.KEY_FILE_ID)

                            if (driveLink != null && fileId != null) {
                                _uploadStatus.value = UploadStatus.Success(driveLink, fileId)
                                Log.d("FeedbackScreenModel", "Drive upload succeeded: $driveLink")
                            } else {
                                _uploadStatus.value = UploadStatus.Failed("No link returned from upload")
                                Log.e("FeedbackScreenModel", "Drive upload succeeded but no link returned")
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            _uploadStatus.value = UploadStatus.Failed("Upload failed")
                            Log.e("FeedbackScreenModel", "Drive upload failed")
                        }
                        WorkInfo.State.CANCELLED -> {
                            _uploadStatus.value = UploadStatus.Failed("Upload cancelled")
                            Log.w("FeedbackScreenModel", "Drive upload cancelled")
                        }
                        else -> {
                            // Handle other states if needed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FeedbackScreenModel", "Error observing Drive upload work", e)
                _uploadStatus.value = UploadStatus.Failed("Error tracking upload: ${e.message}")
            }
        }
    }

    /**
     * ✅ UPDATED: Enqueue GitHub issue creation and track its status
     */
    fun enqueueIssueProcessing(
        title: String,
        body: String,
        labels: Array<String>,
        imageUri: Uri?,
        driveLink: String? = null
    ) {
        screenModelScope.launch {
            try {
                Log.d("FeedbackScreenModel", "Submitting feedback:")
                Log.d("FeedbackScreenModel", "  Title: $title")
                Log.d("FeedbackScreenModel", "  Has Drive Link: ${driveLink != null}")
                Log.d("FeedbackScreenModel", "  Has Image URI: ${imageUri != null}")

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val inputData = workDataOf(
                    "ISSUE_TITLE" to title,
                    "ISSUE_BODY" to body,
                    "ISSUE_LABELS" to labels,
                    "IMAGE_URI" to imageUri.toString(),
                    "DRIVE_LINK" to (driveLink ?: "")
                )

                val request = OneTimeWorkRequestBuilder<FeedbackWorker>()
                    .setInputData(inputData)
                    .setConstraints(constraints)
                    .build()

                workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)

                // ✅ NEW: Set status to submitting and observe the work
                _submissionStatus.value = SubmissionStatus.Submitting
                observeSubmissionWork(request.id.toString())

                Log.d("FeedbackScreenModel", "GitHub issue creation enqueued: ${request.id}")

                // Reset upload state after submission starts
                _uploadWorkId.value = null
                _uploadStatus.value = UploadStatus.Idle

            } catch (e: Exception) {
                Log.e("FeedbackScreenModel", "Error submitting feedback", e)
                _submissionStatus.value = SubmissionStatus.Failed("Submission error: ${e.message}")
            }
        }
    }

    /**
     * ✅ NEW: Observe GitHub issue submission status
     */
    private fun observeSubmissionWork(workId: String) {
        screenModelScope.launch {
            try {
                val uuid = UUID.fromString(workId)

                workManager.getWorkInfoByIdFlow(uuid).collect { workInfo ->
                    when (workInfo?.state) {
                        WorkInfo.State.ENQUEUED -> {
                            _submissionStatus.value = SubmissionStatus.Submitting
                            Log.d("FeedbackScreenModel", "Issue creation enqueued")
                        }
                        WorkInfo.State.RUNNING -> {
                            _submissionStatus.value = SubmissionStatus.Submitting
                            Log.d("FeedbackScreenModel", "Issue creation running...")
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val issueUrl = workInfo.outputData.getString("ISSUE_URL")
                            val issueNumber = workInfo.outputData.getString("ISSUE_NUMBER")

                            _submissionStatus.value = SubmissionStatus.Success(
                                issueUrl = issueUrl ?: "",
                                issueNumber = issueNumber ?: ""
                            )
                            Log.d("FeedbackScreenModel", "Issue created successfully: $issueUrl")
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMessage = workInfo.outputData.getString("ERROR_MESSAGE")
                            _submissionStatus.value = SubmissionStatus.Failed(
                                errorMessage ?: "Issue creation failed"
                            )
                            Log.e("FeedbackScreenModel", "Issue creation failed: $errorMessage")
                        }
                        WorkInfo.State.CANCELLED -> {
                            _submissionStatus.value = SubmissionStatus.Failed("Issue creation cancelled")
                            Log.w("FeedbackScreenModel", "Issue creation cancelled")
                        }
                        else -> {
                            // Handle other states if needed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("FeedbackScreenModel", "Error observing submission work", e)
                _submissionStatus.value = SubmissionStatus.Failed("Error tracking submission: ${e.message}")
            }
        }
    }

    /**
     * Reset upload state (useful if user wants to select different image)
     */
    fun resetUploadState() {
        _uploadWorkId.value?.let { workId ->
            try {
                workManager.cancelWorkById(UUID.fromString(workId))
            } catch (e: Exception) {
                Log.e("FeedbackScreenModel", "Error cancelling upload work", e)
            }
        }
        _uploadWorkId.value = null
        _uploadStatus.value = UploadStatus.Idle
    }

    /**
     * ✅ NEW: Reset submission state
     */
    fun resetSubmissionState() {
        _submissionStatus.value = SubmissionStatus.Idle
    }

    override fun onDispose() {
        super.onDispose()
        // Cancel any ongoing uploads when screen is disposed
        _uploadWorkId.value?.let { workId ->
            try {
                workManager.cancelWorkById(UUID.fromString(workId))
            } catch (e: Exception) {
                Log.e("FeedbackScreenModel", "Error cancelling upload on dispose", e)
            }
        }
    }

    /**
     * Upload status sealed class
     */
    sealed class UploadStatus {
        object Idle : UploadStatus()
        object Uploading : UploadStatus()
        data class Success(val driveLink: String, val fileId: String) : UploadStatus()
        data class Failed(val reason: String) : UploadStatus()
    }

    /**
     * ✅ NEW: Submission status sealed class
     */
    sealed class SubmissionStatus {
        object Idle : SubmissionStatus()
        object Submitting : SubmissionStatus()
        data class Success(val issueUrl: String, val issueNumber: String) : SubmissionStatus()
        data class Failed(val reason: String) : SubmissionStatus()
    }

    companion object {
        const val WORK_NAME = "unique_feedback_work"
    }
}