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
import java.io.File

data class StorageInfo(
    /** Raw bytes used by the notes database and app data files. */
    val notesBytes: Long = 0L,
    /** Raw bytes of cached media (images, videos, documents). */
    val mediaBytes: Long = 0L,
    /** Raw bytes in the system cache dir. */
    val cacheBytes: Long = 0L,
    /** Total quota in bytes shown on the bar (default 5 GB). */
    val quotaBytes: Long = 5L * 1024 * 1024 * 1024
) {
    val usedBytes: Long get() = notesBytes + mediaBytes + cacheBytes
    val freeBytes: Long get() = (quotaBytes - usedBytes).coerceAtLeast(0L)
}

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
            // Notes DB + shared prefs + app files (everything in dataDir except cache)
            val notesBytes = context.dataDir
                ?.walkTopDown()
                ?.filter { it.isFile && !it.path.contains("/cache") }
                ?.sumOf { it.length() } ?: 0L

            // External app-specific files (downloaded media, documents)
            val mediaBytes = (context.getExternalFilesDir(null)
                ?.walkTopDown()
                ?.filter { it.isFile }
                ?.sumOf { it.length() } ?: 0L) +
                    (context.filesDir
                        ?.walkTopDown()
                        ?.filter { it.isFile }
                        ?.sumOf { it.length() } ?: 0L)

            // System cache
            val cacheBytes = (context.cacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L) +
                    (context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L)

            _storageInfo.value = StorageInfo(
                notesBytes = notesBytes,
                mediaBytes = mediaBytes,
                cacheBytes = cacheBytes
            )
        }
    }

    fun refreshStorage() = computeStorage()

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