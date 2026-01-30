package zouitel.neighborhood.feedback

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import zouitel.neighborhood.feedback.Contents.API_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import kotlinx.coroutines.delay

class FeedbackWorker(
    private val context: Context,
    params: WorkerParameters,
    private val githubClient: HttpClient,
    private val repoOwner: String,
    private val repoName: String
): CoroutineWorker(context, params) {

    private val imageUploader = GitHubImageUploader(
        client = githubClient,
        repoOwner = repoOwner,
        repoName = repoName,
        branch = "main"
    )

    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString("ISSUE_TITLE") ?: return Result.failure()
            val body = inputData.getString("ISSUE_BODY") ?: return Result.failure()
            val labels = inputData.getStringArray("ISSUE_LABELS") ?: return Result.failure()
            val imageUriString = inputData.getString("IMAGE_URI")
            val driveLink = inputData.getString("DRIVE_LINK") // ✅ NEW: Get Drive link

            Log.d("FeedbackWorker", "Processing feedback issue:")
            Log.d("FeedbackWorker", "  Title: $title")
            Log.d("FeedbackWorker", "  Has Drive Link: ${!driveLink.isNullOrEmpty()}")
            Log.d("FeedbackWorker", "  Has Image URI: ${!imageUriString.isNullOrEmpty()}")

            // ✅ NEW: Determine which screenshot source to use
            var githubImageUrl: String? = null

            // Priority: Use Drive link if available (better quality, permanent)
            // Fallback: Upload to GitHub if no Drive link
            if (driveLink.isNullOrEmpty() || driveLink == "null") {
                // No Drive link, try uploading to GitHub
                if (!imageUriString.isNullOrEmpty() && imageUriString != "null") {
                    Log.d("FeedbackWorker", "No Drive link, uploading to GitHub...")
                    githubImageUrl = uploadImageToGitHub(imageUriString)
                }
            } else {
                Log.d("FeedbackWorker", "Using Drive link: $driveLink")
            }

            delay(2000) // Brief delay for GitHub API rate limiting

            // ✅ Create professional GitHub issue with Drive link or GitHub image
            val response = createProfessionalIssue(
                title = title,
                body = body,
                labels = labels,
                driveLink = if (!driveLink.isNullOrEmpty() && driveLink != "null") driveLink else null,
                githubImageUrl = githubImageUrl
            )

            if (response != null) {
                Log.d("FeedbackWorker", "Issue created successfully: ${response.html_url}")
                val outputData = Data.Builder()
                    .putString("ISSUE_URL", response.html_url ?: "")
                    .putString("ISSUE_NUMBER", response.number?.toString() ?: "")
                    .build()
                Result.success(outputData)
            } else {
                Log.e("FeedbackWorker", "Failed to create issue")
                val outputData = Data.Builder()
                    .putString("ERROR_MESSAGE", "Failed to create issue")
                    .build()
                Result.failure(outputData)
            }

        } catch (e: Exception) {
            Log.e("FeedbackWorker", "Error in doWork", e)
            e.printStackTrace()
            val outputData = Data.Builder()
                .putString("ERROR_MESSAGE", e.message ?: "Unknown error")
                .build()
            Result.failure(outputData)
        }
    }

    /**
     * Uploads image to GitHub repository and returns the public URL
     * Returns null if upload fails
     */
    @WorkerThread
    private suspend fun uploadImageToGitHub(imageUriString: String): String? {
        return try {
            Log.d("FeedbackWorker", "Uploading image to GitHub: $imageUriString")
            val imageUri = imageUriString.toUri()
            val url = imageUploader.uploadImage(context, imageUri)
            Log.d("FeedbackWorker", "GitHub upload successful: $url")
            url
        } catch (e: Exception) {
            Log.e("FeedbackWorker", "GitHub image upload failed", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * ✅ NEW: Creates a professional GitHub issue with better formatting
     * Supports both Drive links and GitHub-uploaded images
     */
    @WorkerThread
    private suspend fun createProfessionalIssue(
        title: String,
        body: String,
        labels: Array<String>,
        driveLink: String? = null,
        githubImageUrl: String? = null
    ): FeedbackResponse? {
        return try {
            val feedbackBody = buildProfessionalIssueBody(
                userDescription = body,
                driveLink = driveLink,
                githubImageUrl = githubImageUrl
            )

            val requestBody = FeedbackRequest(
                title = title,
                body = feedbackBody,
                labels = labels
            )

            Log.d("FeedbackWorker", "Creating GitHub issue...")
            val response: FeedbackResponse = withContext(Dispatchers.IO) {
                githubClient.post(API_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }.body()
            }

            Log.d("FeedbackWorker", "GitHub issue created: #${response.number}")
            response

        } catch (e: ClientRequestException) {
            Log.e("FeedbackWorker", "GitHub API error: ${e.response.status}", e)
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("FeedbackWorker", "Error creating issue", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * ✅ NEW: Build a professional, well-formatted GitHub issue body
     */
    private fun buildProfessionalIssueBody(
        userDescription: String,
        driveLink: String?,
        githubImageUrl: String?
    ): String {
        return buildString {
            // Main description
            append("## Description\n\n")
            append(userDescription.trim())
            append("\n\n")

            // Screenshot section
            if (driveLink != null || githubImageUrl != null) {
                append("---\n\n")
                append("## 📸 Screenshot\n\n")

                when {
                    // Prefer Drive link (better quality, permanent)
                    driveLink != null -> {
                        append("> **[View Screenshot in Google Drive]($driveLink)**\n\n")
                        append("*Screenshot uploaded to Google Drive for better quality and permanent access*\n\n")
                    }
                    // Fallback to GitHub-hosted image
                    githubImageUrl != null -> {
                        append("![Screenshot]($githubImageUrl)\n\n")
                        append("*Screenshot uploaded to GitHub repository*\n\n")
                    }
                }
            }

            // Device information
            append("---\n\n")
            append("## 📱 Device Information\n\n")
            append("| Property | Value |\n")
            append("|----------|-------|\n")
            append("| **Device** | ${Build.MANUFACTURER} ${Build.MODEL} |\n")
            append("| **Android Version** | ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) |\n")
            append("| **App Version** | ${getAppVersion()} |\n")
            append("| **Submitted At** | ${getCurrentTimestamp()} |\n")

            // Additional context
            append("\n---\n\n")
            append("<sub>*This issue was automatically created via the in-app feedback system*</sub>\n")
        }
    }

    /**
     * Get app version name and code
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "$versionName ($versionCode)"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Get current timestamp in readable format
     */
    private fun getCurrentTimestamp(): String {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", java.util.Locale.US)
            sdf.format(java.util.Date())
        } catch (e: Exception) {
            "Unknown"
        }
    }
}