package zouitel.neighborhood.account.ui

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.provider.Settings
import android.provider.Settings.ACTION_ADD_ACCOUNT
import android.provider.Settings.ACTION_SYNC_SETTINGS
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel
import zouitel.neighborhood.domain.utils.withAsync
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import zouitel.neighborhood.account.model.StorageInfo

@Stable
class AccountScreenModel(
    private val context: Context,
    private val auth: FirebaseAuth
): ScreenModel {
    private val _currentUser: MutableStateFlow<FirebaseUser?> = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _storageInfo: MutableStateFlow<StorageInfo> = MutableStateFlow(StorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
        computeStorage()
    }

    private fun computeStorage() {
        withAsync {
            // Count internal app data once, excluding cache directories. filesDir is
            // already nested under dataDir and must not be added a second time.
            val internalCachePrefix = context.cacheDir.absolutePath + java.io.File.separator
            val codeCachePrefix = context.codeCacheDir.absolutePath + java.io.File.separator
            val notesBytes = context.dataDir
                ?.walkTopDown()
                ?.filter { file ->
                    file.isFile &&
                        !file.absolutePath.startsWith(internalCachePrefix) &&
                        !file.absolutePath.startsWith(codeCachePrefix)
                }
                ?.sumOf { it.length() } ?: 0L

            // External app-specific files are separate from internal dataDir.
            val mediaBytes = context.getExternalFilesDir(null)
                ?.walkTopDown()
                ?.filter { it.isFile }
                ?.sumOf { it.length() } ?: 0L

            // Cache is reported separately and excluded from the app-data total.
            val cacheBytes = (context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }) +
                    (context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L)

            _storageInfo.value = StorageInfo(
                notesBytes = notesBytes,
                mediaBytes = mediaBytes,
                cacheBytes = cacheBytes
            )
        }
    }

    fun manageAccountOnThisDevice() {
        val intent = Intent(ACTION_SYNC_SETTINGS)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun addAnotherAccount() {
        val intent = Intent(ACTION_ADD_ACCOUNT)
            .putExtra(
                Settings.EXTRA_ACCOUNT_TYPES,
                arrayOf("com.google")
            )
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun signOut() {
        withAsync {
            auth.signOut()
        }
    }
}