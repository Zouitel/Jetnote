package zouitel.neighborhood.account

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
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.api.services.drive.model.File as DriveFile

@Stable
class SyncScreenModel(
    application: Application,
    private val context: Context,
    private val auth: FirebaseAuth,
    private val combineUser: DataUseCase.CombineUser
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

    // 🔥 NEW: Track if initial sync is needed
    private val _needsInitialSync = MutableStateFlow(false)
    val needsInitialSync = _needsInitialSync.asStateFlow()

    init {
        setupAuthStateListener()

        screenModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(UPLOAD_WORK)
                .collect { workInfos ->
                    val workInfo = workInfos.any { it.state == WorkInfo.State.RUNNING }
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
    }

    // 🔥 NEW: Setup Firebase Auth state listener with auto-sync
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
                _userState.value = user

                Log.d("SyncScreenModel", "User state updated: ${user?.email ?: "null"}")

                // 🔥 NEW: Auto-download when user signs in
                if (user != null && previousUser == null) {
                    Log.d("SyncScreenModel", "User just signed in, starting auto-sync")
                    autoSyncOnSignIn()
                }
            }
        }
    }

    // 🔥 NEW: Auto sync when user signs in
    // REMOVE auto-worker triggering from autoSyncOnSignIn
    private fun autoSyncOnSignIn() {
        screenModelScope.launch {
            try {
                Log.d("SyncScreenModel", "=== Starting Auto-Sync ===")

                val driveService = initializeDriveService()

                if (driveService != null) {
                    // Only create folders, don't start workers yet
                    createDriveFolders(driveService)

                    // 🔥 NOW start workers after folders are created
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
//    private fun autoSyncOnSignIn() {
//        screenModelScope.launch {
//            try {
//                Log.d("SyncScreenModel", "=== Starting Auto-Sync ===")
//
//                // Initialize Drive service
//                val driveService = initializeDriveService()
//
//                if (driveService != null) {
//                    // Create folders if they don't exist
//                    createDriveFolders(driveService)
//
//                    // Start download process
//                    Log.d("SyncScreenModel", "Starting downloads...")
//                    enqueueDownloadsProcessing()
//
//                    // Also sync to ensure everything is up to date
//                    enqueueSyncProcessing()
//
//                    _needsInitialSync.value = true
//                } else {
//                    Log.e("SyncScreenModel", "Failed to initialize Drive service")
//                }
//            } catch (e: Exception) {
//                Log.e("SyncScreenModel", "Error during auto-sync", e)
//            }
//        }
//    }

    suspend fun initializeDriveService(): Drive? = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(DriveScopes.DRIVE_FILE)
            )

            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            credential.selectedAccount = lastAccount?.account

            credential?.run {
                Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                ).setApplicationName("Drive File Manager").build()
            }
        } catch (e: Exception) {
            Log.e("SyncScreenModel", "Error initializing Drive service", e)
            null
        }
    }
    fun resetInitialSyncFlag() {
        _needsInitialSync.value = false
    }
    private suspend fun findFolderId(
        driveService: Drive,
        folderName: String,
        parentFolderId: String = "root"
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val query =
                    "mimeType='application/vnd.google-apps.folder' and name='$folderName' and '$parentFolderId' in parents and trashed=false"
                val files = driveService.files().list().setQ(query).setFields("files(id)").execute()
                files.files?.firstOrNull()?.id
            } catch (e: Exception) {
                Log.e("SyncScreenModel", "Error finding folder $folderName: ${e.message}")
                null
            }
        }

    suspend fun createDriveFolders(driveService: Drive?) = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext

        var dataFolderId = findFolderId(driveService, "jetnote_data")
        if (dataFolderId == null) {
            dataFolderId = createRootFolder(driveService, "jetnote_data", "root")
        }

        var mediasFolderId = findFolderId(driveService, "jetnote_medias")
        if (mediasFolderId == null) {
            mediasFolderId = createRootFolder(driveService, "jetnote_medias", "root")
        }

        val picturesFolder = "pictures"
        if (mediasFolderId != null) {
            findFolderId(driveService, picturesFolder, mediasFolderId) ?: createMediaFolder(
                driveService,
                picturesFolder,
                mediasFolderId
            )
        }
        val moviesFolder = "movies"
        if (mediasFolderId != null) {
            findFolderId(driveService, moviesFolder, mediasFolderId) ?: createMediaFolder(
                driveService,
                moviesFolder,
                mediasFolderId
            )
        }
        val downloadsFolder = "downloads"
        if (mediasFolderId != null) {
            findFolderId(driveService, downloadsFolder, mediasFolderId) ?: createMediaFolder(
                driveService,
                downloadsFolder,
                mediasFolderId
            )
        }
        val audiosFolder = "audios"
        if (mediasFolderId != null) {
            findFolderId(driveService, audiosFolder, mediasFolderId) ?: createMediaFolder(
                driveService,
                audiosFolder,
                mediasFolderId
            )
        }
    }

    private suspend fun createRootFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String? =
        withContext(Dispatchers.IO) {
            val folderMetadata = DriveFile().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentFolderId)
            }
            try {
                val createdFolder =
                    driveService.files().create(folderMetadata).setFields("id").execute()
                createdFolder?.id
            } catch (e: Exception) {
                Log.e("SyncScreenModel", "Error creating folder $folderName: ${e.message}")
                null
            }
        }

    private suspend fun createMediaFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String? =
        withContext(Dispatchers.IO) {
            val folderMetadata = DriveFile().apply {
                name = folderName
                mimeType = "application/vnd.google-apps.folder"
                parents = listOf(parentFolderId)
            }
            try {
                val createdFolder =
                    driveService.files().create(folderMetadata).setFields("id").execute()
                createdFolder?.id
            } catch (e: Exception) {
                Log.e("SyncScreenModel", "Error creating folder $folderName: ${e.message}")
                null
            }
        }

    fun enqueueUploadsProcessing() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request_data = OneTimeWorkRequestBuilder<DataUploadWorker>()
            .setConstraints(constraints)
            .build()
        val request_others = OneTimeWorkRequestBuilder<OthersUploadWorker>()
            .setConstraints(constraints)
            .build()
        val request_pictures = OneTimeWorkRequestBuilder<PicturesUploadWorker>()
            .setConstraints(constraints)
            .build()
        val request_movies = OneTimeWorkRequestBuilder<MoviesUploadWorker>()
            .setConstraints(constraints)
            .build()
        val request_audios = OneTimeWorkRequestBuilder<AudiosUploadWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            UPLOAD_WORK, KEEP,
            listOf(request_data, request_others, request_pictures, request_movies, request_audios)
        )
    }

    fun enqueueDownloadsProcessing() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request_pictuers = OneTimeWorkRequestBuilder<PicturesDownloadsWorker>()
            .setConstraints(constraints)
            .build()
        val request_movies = OneTimeWorkRequestBuilder<MoviesDownloadsWorker>()
            .setConstraints(constraints)
            .build()
        val request_others = OneTimeWorkRequestBuilder<OthersDownloadsWorker>()
            .setConstraints(constraints)
            .build()
        val request_audios = OneTimeWorkRequestBuilder<AudiosDownloadsWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            DOWNLOAD_WORK, KEEP,
            listOf(request_pictuers, request_movies, request_others, request_audios)
        )
    }

    fun enqueueSyncProcessing() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncDataWorker>()
            .setConstraints(constraints)
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