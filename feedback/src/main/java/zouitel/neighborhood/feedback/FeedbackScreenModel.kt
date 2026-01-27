package zouitel.neighborhood.feedback

import android.app.Application
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cafe.adriel.voyager.core.model.ScreenModel

class FeedbackScreenModel(application: Application): ScreenModel {

    private var workManager = WorkManager.getInstance(application)

    fun enqueueIssueProcessing(title: String, body: String,labels: Array<String>, imageUri: Uri?) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf(
            "ISSUE_TITLE" to title,
            "ISSUE_BODY" to body,
            "ISSUE_LABELS" to labels,
            "IMAGE_URI" to imageUri.toString()
        )

        val request = OneTimeWorkRequestBuilder<FeedbackWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }
    companion object {
        const val WORK_NAME = "unique_feedback_work"
    }
}