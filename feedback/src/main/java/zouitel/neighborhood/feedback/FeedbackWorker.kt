package zouitel.neighborhood.feedback

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import zouitel.neighborhood.feedback.Contents.API_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.withContext
import kotlin.Result as KResult
import kotlinx.coroutines.Dispatchers

class FeedbackWorker(
    context: Context,
    params: WorkerParameters,
    private val client: HttpClient
): CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString("ISSUE_TITLE") ?: return Result.failure()
            val body = inputData.getString("ISSUE_BODY") ?: return Result.failure()
            val labels = inputData.getStringArray("ISSUE_LABELS") ?: return Result.failure()
            val imageUri = inputData.getString("IMAGE_URI")

            createIssue(title, body, labels = labels, imageUri)

            val outputData = Data.Builder()
                .build()

            Result.success(outputData)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            client.close()
        }
    }

    @WorkerThread
    private suspend fun createIssue(
        title: String,
        body: String,
        labels: Array<String>,
        imageUrl: String? = null,
    ): KResult<FeedbackResponse> {
        return try {
            val feedbackBody = if (imageUrl != null) {
                body.plus("\n\nLabel: ${labels.joinToString(", ")}").plus("\n\n").plus("![image]($imageUrl)")
            } else {
                body
            }

            val requestBody = FeedbackRequest(
                title = title,
                body = feedbackBody,
                labels = labels
            )

            val response: FeedbackResponse = withContext(Dispatchers.IO) {
                client.post(API_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()
            }

            KResult.success(response)

        } catch (e: Exception) {
            e.printStackTrace()
            KResult.failure(e)
        }
    }
}