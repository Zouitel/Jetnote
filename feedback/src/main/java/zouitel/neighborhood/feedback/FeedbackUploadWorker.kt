package zouitel.neighborhood.feedback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.drive.model.Permission
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File as JavaFile
import java.io.FileOutputStream
import androidx.core.net.toUri

class FeedbackUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val ioDispatcher: CoroutineDispatcher
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_SCREENSHOT_URI = "screenshot_uri"
        const val KEY_SCREENSHOT_NAME = "screenshot_name"
        const val KEY_DRIVE_LINK = "drive_link"
        const val KEY_FILE_ID = "file_id"
    }

    override suspend fun doWork(): Result {
        return try {
            val screenshotUriString = inputData.getString(KEY_SCREENSHOT_URI)
            val screenshotName = inputData.getString(KEY_SCREENSHOT_NAME) ?: "feedback_screenshot_${System.currentTimeMillis()}.png"

            if (screenshotUriString == null) {
                Log.e("FeedbackUploadWorker", "Screenshot URI is null")
                return Result.failure()
            }

            val screenshotUri = screenshotUriString.toUri()
            val driveService = initializeDriveService()

            if (driveService == null) {
                Log.e("FeedbackUploadWorker", "Failed to initialize Drive service")
                return Result.failure()
            }

            // Upload screenshot and get the link
            val driveLink = uploadScreenshotAndGetLink(
                context = applicationContext,
                driveService = driveService,
                screenshotUri = screenshotUri,
                fileName = screenshotName
            )

            if (driveLink != null) {
                Log.d("FeedbackUploadWorker", "Screenshot uploaded successfully: $driveLink")
                
                // Return the Drive link in the output data
                val outputData = workDataOf(
                    KEY_DRIVE_LINK to driveLink.first,
                    KEY_FILE_ID to driveLink.second
                )
                Result.success(outputData)
            } else {
                Log.e("FeedbackUploadWorker", "Failed to upload screenshot")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error in doWork: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun initializeDriveService(): Drive? = withContext(ioDispatcher) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                applicationContext,
                listOf(DriveScopes.DRIVE_FILE)
            )

            val lastAccount = GoogleSignIn.getLastSignedInAccount(applicationContext)
            if (lastAccount == null) {
                Log.e("FeedbackUploadWorker", "No Google account found")
                return@withContext null
            }

            credential.selectedAccount = lastAccount.account

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Drive File Manager").build()
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Failed to initialize Drive service: ${e.message}", e)
            null
        }
    }

    /**
     * Upload screenshot to Drive and return shareable link
     * @return Pair<WebViewLink, FileId> or null if failed
     */
    private suspend fun uploadScreenshotAndGetLink(
        context: Context,
        driveService: Drive,
        screenshotUri: Uri,
        fileName: String
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            // Find or create feedback folder
            val feedbackFolderId = findOrCreateFeedbackFolder(driveService)
            if (feedbackFolderId == null) {
                Log.e("FeedbackUploadWorker", "Failed to find or create feedback folder")
                return@withContext null
            }

            // Copy URI to temporary file
            val tempFile = copyUriToTempFile(context, screenshotUri, fileName)
            if (tempFile == null || !tempFile.exists()) {
                Log.e("FeedbackUploadWorker", "Failed to create temp file from URI")
                return@withContext null
            }

            try {
                // Check if file already exists
                val existingFileId = findFileInFolder(driveService, fileName, feedbackFolderId)

                val fileId = if (existingFileId != null) {
                    // Update existing file
                    Log.d("FeedbackUploadWorker", "Updating existing file: $fileName")
                    updateExistingFile(driveService, existingFileId, tempFile)
                } else {
                    // Create new file
                    Log.d("FeedbackUploadWorker", "Creating new file: $fileName")
                    createNewFile(driveService, tempFile, fileName, feedbackFolderId)
                }

                if (fileId == null) {
                    Log.e("FeedbackUploadWorker", "Failed to upload file")
                    return@withContext null
                }

                // Make file publicly accessible and get link
                val webViewLink = makeFilePublicAndGetLink(driveService, fileId)
                
                if (webViewLink != null) {
                    Log.d("FeedbackUploadWorker", "File uploaded successfully. Link: $webViewLink")
                    Pair(webViewLink, fileId)
                } else {
                    Log.e("FeedbackUploadWorker", "Failed to get shareable link")
                    null
                }
            } finally {
                // Clean up temp file
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error uploading screenshot: ${e.message}", e)
            null
        }
    }

    /**
     * Find or create the jetnote_feedback folder
     */
    private suspend fun findOrCreateFeedbackFolder(driveService: Drive): String? = 
        withContext(Dispatchers.IO) {
            try {
                // First, try to find existing folder
                var folderId = findFolderId(driveService, "jetnote_feedback")
                
                if (folderId == null) {
                    // Create folder if it doesn't exist
                    Log.d("FeedbackUploadWorker", "Creating jetnote_feedback folder")
                    folderId = createFolder(driveService, "jetnote_feedback", "root")
                }
                
                folderId
            } catch (e: Exception) {
                Log.e("FeedbackUploadWorker", "Error finding/creating feedback folder: ${e.message}", e)
                null
            }
        }

    /**
     * Copy URI content to a temporary file
     */
    private suspend fun copyUriToTempFile(
        context: Context,
        uri: Uri,
        fileName: String
    ): JavaFile? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("FeedbackUploadWorker", "Failed to open input stream from URI")
                return@withContext null
            }

            val tempFile = JavaFile(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.use { input ->
                    input.copyTo(outputStream)
                }
            }

            Log.d("FeedbackUploadWorker", "Temp file created: ${tempFile.absolutePath}, size: ${tempFile.length()} bytes")
            tempFile
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error copying URI to temp file: ${e.message}", e)
            null
        }
    }

    /**
     * Create a new file in Drive
     */
    private suspend fun createNewFile(
        driveService: Drive,
        localFile: JavaFile,
        fileName: String,
        folderId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileMetadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
            }

            val mimeType = getMimeType(fileName)
            val mediaContent = FileContent(mimeType, localFile)

            val createRequest = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink, webContentLink")
            
            // Configure chunked upload for large files
            createRequest.mediaHttpUploader?.apply {
                isDirectUploadEnabled = false
                chunkSize = 10 * 1024 * 1024 // 10MB chunks
            }

            val file = createRequest.execute()
            Log.d("FeedbackUploadWorker", "File created with ID: ${file.id}")
            file.id
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error creating file: ${e.message}", e)
            null
        }
    }

    /**
     * Update an existing file in Drive
     */
    private suspend fun updateExistingFile(
        driveService: Drive,
        fileId: String,
        localFile: JavaFile
    ): String? = withContext(Dispatchers.IO) {
        try {
            val mimeType = getMimeType(localFile.name)
            val mediaContent = FileContent(mimeType, localFile)

            val updateRequest = driveService.files().update(fileId, null, mediaContent)
                .setFields("id, webViewLink, webContentLink")
            
            // Configure chunked upload for large files
            updateRequest.mediaHttpUploader?.apply {
                isDirectUploadEnabled = false
                chunkSize = 10 * 1024 * 1024 // 10MB chunks
            }

            val file = updateRequest.execute()
            Log.d("FeedbackUploadWorker", "File updated with ID: ${file.id}")
            file.id
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error updating file: ${e.message}", e)
            null
        }
    }

    /**
     * Make file publicly accessible and get shareable link
     */
    private suspend fun makeFilePublicAndGetLink(
        driveService: Drive,
        fileId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Create public permission
            val permission = Permission().apply {
                type = "anyone"
                role = "reader"
            }

            // Apply permission
            driveService.permissions().create(fileId, permission)
                .setFields("id")
                .execute()

            // Get file metadata with links
            val file = driveService.files().get(fileId)
                .setFields("webViewLink, webContentLink")
                .execute()

            // Prefer webViewLink (opens in Drive viewer) over webContentLink (direct download)
            val link = file.webViewLink ?: file.webContentLink
            
            Log.d("FeedbackUploadWorker", "File made public. Link: $link")
            link
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error making file public: ${e.message}", e)
            null
        }
    }

    /**
     * Find a folder by name
     */
    private suspend fun findFolderId(
        driveService: Drive,
        folderName: String,
        parentId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val query = if (parentId != null) {
                "name='$folderName' and '$parentId' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false"
            } else {
                "name='$folderName' and mimeType='application/vnd.google-apps.folder' and trashed=false"
            }

            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name)")
                .setPageSize(10)
                .setOrderBy("createdTime")
                .execute()

            val folders = result.files
            if (folders != null && folders.size > 1) {
                Log.w("FeedbackUploadWorker", "Found ${folders.size} folders named '$folderName'. Using oldest.")
            }

            folders?.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Failed to find folder '$folderName': ${e.message}", e)
            null
        }
    }

    /**
     * Find a file in a specific folder
     */
    private suspend fun findFileInFolder(
        driveService: Drive,
        fileName: String,
        folderId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val query = "name='$fileName' and '$folderId' in parents and trashed=false"
            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute()

            result.files?.firstOrNull()?.id
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Error finding file: ${e.message}", e)
            null
        }
    }

    /**
     * Create a folder in Drive
     */
    private suspend fun createFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val folderMetadata = DriveFile().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentFolderId)
            }

            val folder = driveService.files()
                .create(folderMetadata)
                .setFields("id, name")
                .execute()

            Log.d("FeedbackUploadWorker", "Created folder '$folderName' with ID: ${folder.id}")
            folder.id
        } catch (e: Exception) {
            Log.e("FeedbackUploadWorker", "Failed to create folder '$folderName': ${e.message}", e)
            null
        }
    }

    /**
     * Get the appropriate MIME type for image files
     */
    private fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            else -> "image/png" // Default to PNG for screenshots
        }
    }
}