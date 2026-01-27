package zouitel.neighborhood.account

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

@Stable
class AccountScreenModel(
    private val context: Context,
    private val auth: FirebaseAuth
): ScreenModel {
    private val _currentUser: MutableStateFlow<FirebaseUser?> = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
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