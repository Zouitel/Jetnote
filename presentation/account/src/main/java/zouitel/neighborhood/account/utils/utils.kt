package zouitel.neighborhood.account.utils

import com.google.api.services.drive.Drive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun findFolderId(driveService: Drive, folderName: String): String? = withContext(Dispatchers.IO) {
    try {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and 'root' in parents and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
        result.files?.firstOrNull()?.id
    } catch (e: Exception) {
        null
    }
}

internal suspend fun findFolderId(driveService: Drive, folderName: String, parentFolderId: String): String? = withContext(Dispatchers.IO) {
    try {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and '$parentFolderId' in parents and trashed=false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id)")
            .execute()
        result.files?.firstOrNull()?.id
    } catch (e: Exception) {
        null
    }
}

