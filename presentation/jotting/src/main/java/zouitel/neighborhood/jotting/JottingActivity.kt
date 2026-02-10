package zouitel.neighborhood.jotting

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import zouitel.neighborhood.audio.player.PlaybackManager
import zouitel.neighborhood.permissions.PermissionScreenModel
import zouitel.neighborhood.jotting.model.Screens
import zouitel.neighborhood.jotting.ui.Jotting
import zouitel.neighborhood.ui.MainTheme
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.model.Data
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import kotlin.uuid.Uuid
import androidx.core.graphics.drawable.toDrawable

class JottingActivity: ComponentActivity(), KoinComponent {

    private var mediaUri = mutableStateOf<Uri?>(null)
    private var isVideo = mutableStateOf(false)
    private var description = mutableStateOf("")
    private var screen = mutableStateOf<Screens?>(null)
    private val permissionModel: PermissionScreenModel by inject()
    private val playerManager: PlaybackManager by inject()
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val settingsModel by inject<SettingsScreenModel>()
    val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        permissionModel.activityBind(this)

        setContent {
            val descriptionState = rememberTextFieldState(description.value)
            val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
            val backgroundColorState = rememberSaveable { mutableIntStateOf(backgroundColor) }
            val textColor = contentColorFor(MaterialTheme.colorScheme.surface).toArgb()
            val textColorState = rememberSaveable { mutableIntStateOf(textColor) }

            val data = remember {
                mutableStateOf(Data(
                    uid = Uuid.random().toString(),
                    userUid = auth.uid.toString(),
                    description = descriptionState.text.toString(),
                    background = backgroundColorState.intValue,
                    textColor = textColorState.intValue
                ))
            }

            MainTheme(settingsModel) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = .6f))
                ) {
                    Jotting(screen, data) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    @SuppressLint("UnsafeIntentLaunch")
    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND)
            when {
                intent.type?.startsWith("text/plain") == true -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        description.value = sharedText
                    }
                }

                intent.type?.startsWith("application/") == true -> {
                    val documentUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (documentUri != null) {
                        println(documentUri)
                    }
                }

                intent.type?.startsWith("audio/") == true -> {
                    val audioUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (audioUri != null) {
                        println(audioUri)
                    }
                }

                intent.type?.startsWith("video/") == true -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (imageUri != null) {
                        mediaUri.value = imageUri
                        isVideo.value = true
                    }
                }

                intent.type?.startsWith("image/") == true -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (imageUri != null) {
                        mediaUri.value = imageUri
                    }
                }
            } else if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
            if (sharedText != null) {
                description.value = sharedText
            }
        } else if (intent?.action == Intent.ACTION_VIEW) {
            if (intent.extras?.containsKey("quick_photo") == true) {
                screen.value = Screens.PHOTO_SCREEN
            } else if (intent.extras?.containsKey("quick_record") == true) {
                screen.value = Screens.RECORD_SCREEN
            } else if (intent.extras?.containsKey("quick_reminder") == true) {
                screen.value = Screens.REMINDER_SCREEN
            } else {
                screen.value = Screens.NOTE_SCREEN
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        intent.removeCategory(Intent.CATEGORY_DEFAULT)
        intent.action = null
        activityScope.cancel()
        playerManager.releaseController()
    }
}