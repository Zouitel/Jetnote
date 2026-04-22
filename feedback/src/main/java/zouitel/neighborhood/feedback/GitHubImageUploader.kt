package zouitel.neighborhood.feedback

import android.content.Context
import android.net.Uri
import android.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Uploads images directly to GitHub repository
 * Images are stored in a dedicated folder (e.g., feedback-screenshots)
 */
class GitHubImageUploader(
    private val client: HttpClient,
    private val repoOwner: String,
    private val repoName: String,
    private val branch: String = "main"
) {

    @Serializable
    data class GitHubUploadRequest(
        val message: String,
        val content: String,
        val branch: String
    )

    @Serializable
    data class GitHubUploadResponse(
        val content: ContentInfo? = null
    )

    @Serializable
    data class ContentInfo(
        val name: String? = null,
        val path: String? = null,
        val sha: String? = null,
        val download_url: String? = null,
        val html_url: String? = null
    )

    /**
     * Uploads an image to GitHub repository and returns the raw URL
     * 
     * @param context Android context to access content resolver
     * @param imageUri Local URI of the image
     * @return Public URL of the uploaded image or null if upload fails
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Read image bytes from URI
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: return@withContext null

                val imageBytes = inputStream.readBytes()
                inputStream.close()

                // Convert to base64
                val base64Content = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                // Generate unique filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val uniqueId = UUID.randomUUID().toString().substring(0, 8)
                val filename = "screenshot_${timestamp}_${uniqueId}.jpg"
                
                // Path in repository (e.g., feedback-screenshots/screenshot_20240129_123456_abc123.jpg)
                val path = "feedback-screenshots/$filename"

                // Create upload request
                val uploadRequest = GitHubUploadRequest(
                    message = "Upload feedback screenshot",
                    content = base64Content,
                    branch = branch
                )

                // Upload to GitHub
                val response: GitHubUploadResponse = client.put(
                    "https://api.github.com/repos/$repoOwner/$repoName/contents/$path"
                ) {
                    contentType(ContentType.Application.Json)
                    setBody(uploadRequest)
                }.body()

                // Return the download URL (raw content URL)
                response.content?.download_url

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Alternative: Get raw.githubusercontent.com URL
     * This is more reliable for displaying in GitHub issues
     */
//    fun getRawUrl(path: String): String {
//        return "https://raw.githubusercontent.com/$repoOwner/$repoName/$branch/$path"
//    }
}