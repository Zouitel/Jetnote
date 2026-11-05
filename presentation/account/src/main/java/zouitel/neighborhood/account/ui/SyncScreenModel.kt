package zouitel.neighborhood.account.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.domain.usecase.DataUseCase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import zouitel.neighborhood.account.worker.AccountWorkIdentity
import zouitel.neighborhood.account.worker.AudiosDownloadsWorker
import zouitel.neighborhood.account.worker.AudiosUploadWorker
import zouitel.neighborhood.account.worker.DataUploadWorker
import zouitel.neighborhood.account.worker.MoviesDownloadsWorker
import zouitel.neighborhood.account.worker.MoviesUploadWorker
import zouitel.neighborhood.account.worker.OthersDownloadsWorker
import zouitel.neighborhood.account.worker.OthersUploadWorker
import zouitel.neighborhood.account.worker.PicturesDownloadsWorker
import zouitel.neighborhood.account.worker.PicturesUploadWorker
import zouitel.neighborhood.account.worker.SyncDataWorker
import zouitel.neighborhood.security.NativeSecurityManager
import com.google.api.services.drive.model.File as DriveFile

@Stable
class SyncScreenModel(
    application: Application,
    private val context: Context,
    private val auth: FirebaseAuth,
    private val combineUser: DataUseCase.CombineUser,
    private val nativeSecurityManager: NativeSecurityManager
): ScreenModel {

    private val _userState = MutableStateFlow(auth.currentUser)
    val userState = _userState.asStateFlow()

    private val workManager = WorkManager.getInstance(application)

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _syncDataState = MutableStateFlow(false)
    val syncDataState = _syncDataState.asStateFlow()

    private val _needsInitialSync = MutableStateFlow(false)
    val needsInitialSync = _needsInitialSync.asStateFlow()

    // ✅ Use Mutex to prevent concurrent folder creation
    private val folderCreationMutex = Mutex()
    private var foldersCreatedThisSession = false

    private val _googleClientId = MutableStateFlow("")
    val googleClientId = _googleClientId.asStateFlow()

    init {
        setupAuthStateListener()

        screenModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(UPLOAD_WORK)
                .collect { workInfos ->
                    val workInfo = workInfos.any {
                        it.state == WorkInfo.State.ENQUEUED ||
                            it.state == WorkInfo.State.RUNNING ||
                            it.state == WorkInfo.State.BLOCKED
                    }
                    _isUploading.value = workInfo
                }
        }
        screenModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(SYNCHRONIZE_WORK)
                .collect { workInfos ->
                    val workInfo = workInfos.any { it.state == WorkInfo.State.RUNNING }
                    _syncDataState.value = workInfo
                }
        }
        screenModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(DOWNLOAD_WORK)
                .collect { workInfos ->
                    val workInfo = workInfos.any { it.state == WorkInfo.State.RUNNING }
                    _isDownloading.value = workInfo
                }
        }

        screenModelScope.launch {
            _googleClientId.value = nativeSecurityManager.googleClientId()
        }
    }

    private fun setupAuthStateListener() {
        screenModelScope.launch {
            callbackFlow {
                val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                    Log.d("SyncScreenModel", "Auth state changed: ${firebaseAuth.currentUser?.email}")
                    trySend(firebaseAuth.currentUser)
                }

                auth.addAuthStateListener(authStateListener)
                trySend(auth.currentUser)

                awaitClose {
                    Log.d("SyncScreenModel", "Removing auth state listener")
                    auth.removeAuthStateListener(authStateListener)
                }
            }.collect { user ->
                val previousUser = _userState.value
                val previousUid = previousUser?.uid
                val currentUid = user?.uid

                if (previousUid != currentUid) {
                    Log.d("SyncScreenModel", "Account changed; cancelling work owned by the previous session")
                    try {
                        cancelAccountWork()
                    } catch (error: Exception) {
                        Log.e("SyncScreenModel", "Could not cancel previous account work; refusing to start sync for the new account", error)
                        return@collect
                    }
                    foldersCreatedThisSession = false
                    _needsInitialSync.value = false
                }

                _userState.value = user
                Log.d("SyncScreenModel", "User state updated: ${user?.email ?: "null"}")

                if (currentUid != null && previousUid != currentUid) {
                    Log.d("SyncScreenModel", "User signed in or switched accounts, starting auto-sync")
                    autoSyncOnSignIn()
                }
            }
        }
    }

    // ✅ CRITICAL FIX: Use Mutex to guarantee only ONE folder creation happens
    private fun autoSyncOnSignIn() {
        screenModelScope.launch {
            folderCreationMutex.withLock {
                try {
                    // Double-check inside the mutex
                    if (foldersCreatedThisSession) {
                        Log.d("SyncScreenModel", "Folders already created in this session, skipping")
                        return@launch
                    }

                    Log.d("SyncScreenModel", "=== Starting Auto-Sync (mutex locked) ===")

                    val driveService = initializeDriveService()
                    if (driveService != null) {
                        createDriveFolders(driveService)
                        foldersCreatedThisSession = true

                        Log.d("SyncScreenModel", "Folders ready, starting workers...")
                        enqueueDownloadsProcessing()
                        enqueueSyncProcessing()

                        _needsInitialSync.value = true
                    } else {
                        Log.e("SyncScreenModel", "Failed to initialize Drive service")
                    }
                } catch (e: Exception) {
                    Log.e("SyncScreenModel", "Error during auto-sync", e)
                }
            }
        }
    }

    suspend fun initializeDriveService(): Drive? = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE)
            )

            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount?.account == null) {
                Log.e("SyncScreenModel", "No Google account found")
                return@withContext null
            }

            credential.selectedAccount = lastAccount.account

            Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("Drive File Manager").build()
        } catch (e: Exception) {
            Log.e("SyncScreenModel", "Error initializing Drive service", e)
            null
        }
    }

    /**
     * Cancel every account-scoped chain before a different Firebase identity can
     * start using the same WorkManager unique-work names.
     */
    private suspend fun cancelAccountWork() = withContext(Dispatchers.IO) {
        // Await cancellation before the next identity enqueues work under these
        // shared unique names; otherwise KEEP could retain the previous account's chain.
        workManager.cancelUniqueWork(UPLOAD_WORK).result.get()
        workManager.cancelUniqueWork(DOWNLOAD_WORK).result.get()
        workManager.cancelUniqueWork(SYNCHRONIZE_WORK).result.get()
    }

    fun resetInitialSyncFlag() {
        _needsInitialSync.value = false
    }

    /**
     * Resolve a folder only when the Drive query completed successfully and found
     * exactly one match. A failed query must not be mistaken for "not found", or
     * get-or-create could create duplicate folder trees.
     */
    private suspend fun findFolderId(
        driveService: Drive,
        folderName: String,
        parentFolderId: String = "root"
    ): String? = withContext(Dispatchers.IO) {
        val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and '$parentFolderId' in parents and trashed=false"
        val folders = mutableListOf<DriveFile>()
        var pageToken: String? = null

        do {
            val request = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, createdTime)")
                .setPageSize(100)

            if (pageToken != null) request.setPageToken(pageToken)

            val page = request.execute()
            folders += page.files.orEmpty()
            pageToken = page.nextPageToken
        } while (pageToken != null)

        when (folders.size) {
            0 -> {
                Log.d("SyncScreenModel", "No folder found named '$folderName'")
                null
            }
            1 -> folders.single().id
            else -> throw IllegalStateException(
                "Found ${folders.size} Drive folders named '$folderName' under parent '$parentFolderId'. Resolve duplicates before syncing."
            )
        }
    }

    /**
     * Do not mark setup complete unless every required root and media folder has
     * a verified Drive ID. Exceptions intentionally propagate to auto-sync.
     */
    suspend fun createDriveFolders(driveService: Drive?) = withContext(Dispatchers.IO) {
        val drive = requireNotNull(driveService) { "Google Drive service is not initialized" }

        Log.d("SyncScreenModel", "Starting folder creation/verification...")

        requireFolder(drive, "jetnote_data", "root")
        val mediasFolderId = requireFolder(drive, "jetnote_medias", "root")
        requireFolder(drive, "jetnote_feedback", "root")

        listOf("pictures", "movies", "downloads", "audios").forEach { folderName ->
            requireFolder(drive, folderName, mediasFolderId)
        }

        Log.d("SyncScreenModel", "Drive folder structure verified successfully")
    }

    private suspend fun requireFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String = getOrCreateFolder(driveService, folderName, parentFolderId)
        ?: throw IllegalStateException("Could not create or verify Drive folder '$folderName'")

    private suspend fun getOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String? = withContext(Dispatchers.IO) {
        findFolderId(driveService, folderName, parentFolderId)?.let { existingId ->
            Log.d("SyncScreenModel", "Folder '$folderName' exists: $existingId")
            return@withContext existingId
        }

        Log.d("SyncScreenModel", "Creating folder: $folderName")
        createFolder(driveService, folderName, parentFolderId)
    }

    private suspend fun createFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String = withContext(Dispatchers.IO) {
        val folderMetadata = DriveFile().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"
            parents = listOf(parentFolderId)
        }

        driveService.files()
            .create(folderMetadata)
            .setFields("id, name")
            .execute()
            .id ?: throw IllegalStateException("Drive did not return an ID for folder '$folderName'")
    }

    fun enqueueUploadsProcessing() {
        val accountWorkData = AccountWorkIdentity.dataFor(auth.currentUser?.uid, context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request_data = OneTimeWorkRequestBuilder<DataUploadWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_others = OneTimeWorkRequestBuilder<OthersUploadWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_pictures = OneTimeWorkRequestBuilder<PicturesUploadWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_movies = OneTimeWorkRequestBuilder<MoviesUploadWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_audios = OneTimeWorkRequestBuilder<AudiosUploadWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()

        workManager.enqueueUniqueWork(
            UPLOAD_WORK, KEEP,
            listOf(request_data, request_others, request_pictures, request_movies, request_audios)
        )
    }

    fun enqueueDownloadsProcessing() {
        val accountWorkData = AccountWorkIdentity.dataFor(auth.currentUser?.uid, context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request_pictuers = OneTimeWorkRequestBuilder<PicturesDownloadsWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_movies = OneTimeWorkRequestBuilder<MoviesDownloadsWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_others = OneTimeWorkRequestBuilder<OthersDownloadsWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()
        val request_audios = OneTimeWorkRequestBuilder<AudiosDownloadsWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()

        workManager.enqueueUniqueWork(
            DOWNLOAD_WORK, KEEP,
            listOf(request_pictuers, request_movies, request_others, request_audios)
        )
    }

    fun enqueueSyncProcessing() {
        val accountWorkData = AccountWorkIdentity.dataFor(auth.currentUser?.uid, context)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncDataWorker>()
            .setConstraints(constraints)
            .setInputData(accountWorkData)
            .build()

        workManager.enqueueUniqueWork(SYNCHRONIZE_WORK, KEEP, request)
    }

    companion object {
        const val UPLOAD_WORK = "unique_uploads_data_work"
        const val DOWNLOAD_WORK = "unique_downloads_data_work"
        const val SYNCHRONIZE_WORK = "unique_synchronize_work"
    }

    override fun onDispose() {
        super.onDispose()
        auth.uid?.let {
            screenModelScope.launch {
                combineUser(it)
            }
        }
    }
}