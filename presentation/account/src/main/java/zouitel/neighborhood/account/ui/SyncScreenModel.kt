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
                _userState.value = user

                Log.d("SyncScreenModel", "User state updated: ${user?.email ?: "null"}")

                if (user != null && previousUser == null) {
                    Log.d("SyncScreenModel", "User just signed in, starting auto-sync")
                    autoSyncOnSignIn()
                } else if (user == null && previousUser != null) {
                    Log.d("SyncScreenModel", "User signed out, resetting folder flag")
                    foldersCreatedThisSession = false
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

    fun resetInitialSyncFlag() {
        _needsInitialSync.value = false
    }

    // ✅ IMPROVED: Better search with pageSize limit and better query
    private suspend fun findFolderId(
        driveService: Drive,
        folderName: String,
        parentFolderId: String = "root"
    ): String? = withContext(Dispatchers.IO) {
        try {
            val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and '$parentFolderId' in parents and trashed=false"

            Log.d("SyncScreenModel", "Searching for folder: $folderName in parent: $parentFolderId")

            val result = driveService.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id, name, createdTime)")
                .setPageSize(10)
                .setOrderBy("createdTime")
                .execute()

            val folders = result.files

            when {
                folders == null || folders.isEmpty() -> {
                    Log.d("SyncScreenModel", "No folder found named '$folderName'")
                    null
                }
                folders.size == 1 -> {
                    Log.d("SyncScreenModel", "Found folder '$folderName': ${folders[0].id}")
                    folders[0].id
                }
                else -> {
                    // ⚠️ Multiple folders found - use the FIRST one (oldest)
                    Log.w("SyncScreenModel", "Found ${folders.size} folders named '$folderName'! Using oldest: ${folders[0].id}")
                    folders[0].id
                }
            }
        } catch (e: Exception) {
            Log.e("SyncScreenModel", "Error finding folder $folderName", e)
            null
        }
    }

    // ✅ IMPROVED: More defensive folder creation
    suspend fun createDriveFolders(driveService: Drive?) = withContext(Dispatchers.IO) {
        if (driveService == null) {
            Log.e("SyncScreenModel", "Drive service is null")
            return@withContext
        }

        Log.d("SyncScreenModel", "Starting folder creation/verification...")

        try {
            // Create root folders
            val dataFolderId = getOrCreateFolder(driveService, "jetnote_data", "root")
            val mediasFolderId = getOrCreateFolder(driveService, "jetnote_medias", "root")
            val feedbackFolderId = getOrCreateFolder(driveService, "jetnote_feedback", "root")

            // Create media subfolders
            if (mediasFolderId != null) {
                getOrCreateFolder(driveService, "pictures", mediasFolderId)
                getOrCreateFolder(driveService, "movies", mediasFolderId)
                getOrCreateFolder(driveService, "downloads", mediasFolderId)
                getOrCreateFolder(driveService, "audios", mediasFolderId)
            }

            Log.d("SyncScreenModel", "✅ Folder structure verified/created successfully")
        } catch (e: Exception) {
            Log.e("SyncScreenModel", "Error during folder creation", e)
            throw e
        }
    }

    // ✅ NEW: Atomic get-or-create operation
    private suspend fun getOrCreateFolder(
        driveService: Drive,
        folderName: String,
        parentFolderId: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // First, try to find existing folder
            var folderId = findFolderId(driveService, folderName, parentFolderId)

            if (folderId != null) {
                Log.d("SyncScreenModel", "✓ Folder '$folderName' exists: $folderId")
                return@withContext folderId
            }

            // Not found, create it
            Log.d("SyncScreenModel", "Creating folder: $folderName")
            folderId = createFolder(driveService, folderName, parentFolderId)

            if (folderId != null) {
                Log.d("SyncScreenModel", "✓ Created folder '$folderName': $folderId")
            } else {
                Log.e("SyncScreenModel", "✗ Failed to create folder '$folderName'")
            }

            return@withContext folderId
        } catch (e: Exception) {
            Log.e("SyncScreenModel", "Error in getOrCreateFolder for '$folderName'", e)
            null
        }
    }

    // ✅ RENAMED for clarity (was createRootFolder and createMediaFolder)
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

            val createdFolder = driveService.files()
                .create(folderMetadata)
                .setFields("id, name")
                .execute()

            createdFolder?.id
        } catch (e: Exception) {
            Log.e("SyncScreenModel", "Failed to create folder '$folderName'", e)
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