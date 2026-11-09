package zouitel.neighborhood.account.worker

import android.content.Context
import android.util.Log
import androidx.work.Data
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Build the Drive service bound to the signed-in Google account and to this work's
 * Firebase identity. Every Drive worker used to carry its own copy of this routine,
 * and the copies had drifted (silent failure, missing-account handling, logging);
 * this factory is the single source of truth.
 */
internal suspend fun initializeDriveService(
    context: Context,
    inputData: Data,
    tag: String,
    ioDispatcher: CoroutineDispatcher,
): Drive? = withContext(ioDispatcher) {
    try {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount == null) {
            Log.w(tag, "No signed-in Google account; cannot create the Drive service")
            return@withContext null
        }

        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = lastAccount.account

        Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            AccountBoundDriveRequestInitializer(credential, context, inputData)
        ).setApplicationName("Drive File Manager").build()
    } catch (e: Exception) {
        Log.e(tag, "Failed to initialize the Drive service: ${e.message}", e)
        null
    }
}
