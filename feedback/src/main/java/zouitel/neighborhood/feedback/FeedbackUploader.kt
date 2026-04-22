//package zouitel.neighborhood.feedback
//
//import android.content.Context
//import android.net.Uri
//import androidx.work.Constraints
//import androidx.work.NetworkType
//import androidx.work.OneTimeWorkRequestBuilder
//import androidx.work.WorkInfo
//import androidx.work.WorkManager
//import androidx.work.workDataOf
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//import zouitel.neighborhood.feedback.worker.FeedbackUploadWorker
//
///**
// * Example usage of FeedbackUploadWorker
// *
// * This class demonstrates how to:
// * 1. Upload a screenshot to Google Drive
// * 2. Get the shareable link back
// * 3. Monitor the upload progress
// */
//class FeedbackUploader(private val context: Context) {
//
//    private val workManager = WorkManager.getInstance(context)
//
//    /**
//     * Upload a screenshot and get the Drive link
//     *
//     * @param screenshotUri The URI of the screenshot to upload
//     * @param fileName Optional custom filename (default: feedback_screenshot_[timestamp].png)
//     * @return WorkInfo ID to track the upload
//     */
//    fun uploadScreenshot(
//        screenshotUri: Uri,
//        fileName: String? = null
//    ): String {
//        val inputData = workDataOf(
//            FeedbackUploadWorker.KEY_SCREENSHOT_URI to screenshotUri.toString(),
//            FeedbackUploadWorker.KEY_SCREENSHOT_NAME to (fileName ?: "feedback_screenshot_${System.currentTimeMillis()}.png")
//        )
//
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .setRequiresStorageNotLow(true)
//            .build()
//
//        val uploadRequest = OneTimeWorkRequestBuilder<FeedbackUploadWorker>()
//            .setInputData(inputData)
//            .setConstraints(constraints)
//            .build()
//
//        workManager.enqueue(uploadRequest)
//
//        return uploadRequest.id.toString()
//    }
//
//    /**
//     * Get the upload status and result as a Flow
//     */
//    fun observeUploadStatus(workId: String): Flow<UploadStatus> {
//        return workManager.getWorkInfoByIdFlow(java.util.UUID.fromString(workId))
//            .map { workInfo ->
//                when {
//                    workInfo == null -> UploadStatus.Unknown
//                    workInfo.state == WorkInfo.State.RUNNING -> UploadStatus.Uploading
//                    workInfo.state == WorkInfo.State.SUCCEEDED -> {
//                        val driveLink = workInfo.outputData.getString(FeedbackUploadWorker.KEY_DRIVE_LINK)
//                        val fileId = workInfo.outputData.getString(FeedbackUploadWorker.KEY_FILE_ID)
//                        if (driveLink != null && fileId != null) {
//                            UploadStatus.Success(driveLink, fileId)
//                        } else {
//                            UploadStatus.Failed("No link returned")
//                        }
//                    }
//                    workInfo.state == WorkInfo.State.FAILED -> UploadStatus.Failed("Upload failed")
//                    workInfo.state == WorkInfo.State.CANCELLED -> UploadStatus.Failed("Upload cancelled")
//                    else -> UploadStatus.Unknown
//                }
//            }
//    }
//
//    sealed class UploadStatus {
//        object Unknown : UploadStatus()
//        object Uploading : UploadStatus()
//        data class Success(val driveLink: String, val fileId: String) : UploadStatus()
//        data class Failed(val reason: String) : UploadStatus()
//    }
//}
//
//// ============================================
//// USAGE EXAMPLE IN A COMPOSABLE OR ACTIVITY
//// ============================================
//
///**
// * Example: How to use in a feedback screen
// */
///*
//@Composable
//fun FeedbackScreen() {
//    val context = LocalContext.current
//    val feedbackUploader = remember { FeedbackUploader(context) }
//
//    var uploadWorkId by remember { mutableStateOf<String?>(null) }
//    var driveLink by remember { mutableStateOf<String?>(null) }
//    var uploadStatus by remember { mutableStateOf("Not started") }
//
//    // Observe upload status
//    uploadWorkId?.let { workId ->
//        val status by feedbackUploader.observeUploadStatus(workId).collectAsState(
//            initial = FeedbackUploader.UploadStatus.Unknown
//        )
//
//        LaunchedEffect(status) {
//            when (status) {
//                is FeedbackUploader.UploadStatus.Uploading -> {
//                    uploadStatus = "Uploading screenshot..."
//                }
//                is FeedbackUploader.UploadStatus.Success -> {
//                    val success = status as FeedbackUploader.UploadStatus.Success
//                    driveLink = success.driveLink
//                    uploadStatus = "Upload complete!"
//                    Log.d("Feedback", "Screenshot link: ${success.driveLink}")
//                }
//                is FeedbackUploader.UploadStatus.Failed -> {
//                    val failed = status as FeedbackUploader.UploadStatus.Failed
//                    uploadStatus = "Upload failed: ${failed.reason}"
//                }
//                else -> {
//                    uploadStatus = "Unknown status"
//                }
//            }
//        }
//    }
//
//    Column(modifier = Modifier.padding(16.dp)) {
//        Text("Feedback Form", style = MaterialTheme.typography.headlineMedium)
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Screenshot capture button
//        Button(onClick = {
//            // Capture screenshot (using your screenshot library)
//            val screenshotUri = captureScreenshot(context)
//
//            // Upload to Drive
//            uploadWorkId = feedbackUploader.uploadScreenshot(screenshotUri)
//        }) {
//            Text("Capture & Upload Screenshot")
//        }
//
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text(uploadStatus)
//
//        driveLink?.let { link ->
//            Spacer(modifier = Modifier.height(8.dp))
//            Text("Screenshot URL: $link", style = MaterialTheme.typography.bodySmall)
//
//            // You can now send this link with your feedback
//            Button(onClick = {
//                sendFeedbackWithScreenshot(link)
//            }) {
//                Text("Submit Feedback")
//            }
//        }
//    }
//}
//
//fun sendFeedbackWithScreenshot(screenshotLink: String) {
//    // Send feedback to your backend with the Drive link
//    val feedbackData = mapOf(
//        "message" to "Bug report",
//        "screenshot" to screenshotLink,
//        "timestamp" to System.currentTimeMillis()
//    )
//
//    // Post to your API
//    // api.submitFeedback(feedbackData)
//}
//*/
