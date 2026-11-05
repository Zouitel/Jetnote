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
import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.workspace.model.Media
import zouitel.neighborhood.workspace.model.MediaType
import zouitel.neighborhood.audio.model.Audio
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
    private var sharedMedias = mutableStateOf<List<Media>>(emptyList())
    private var sharedAudios = mutableStateOf<List<Audio>>(emptyList())
    private var pendingSharedText = mutableStateOf<String?>(null)
    private var pendingSharedMedias = mutableStateOf<List<Media>>(emptyList())
    private var pendingSharedAudios = mutableStateOf<List<Audio>>(emptyList())
    private var shareRevision = mutableIntStateOf(0)
    private var screen = mutableStateOf<Screens?>(null)
    private val permissionModel: PermissionScreenModel by inject()
    private val playerManager: PlaybackManager by inject()
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val settingsModel by inject<SettingsScreenModel>()
    val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            handleIncomingIntent(intent, publishToEditor = false)
        } else {
            restoreSharedContent(savedInstanceState)
        }
        permissionModel.activityBind(this)

        setContent {
//            val descriptionState = rememberTextFieldState(description.value)
            val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
            val backgroundColorState = rememberSaveable { mutableIntStateOf(backgroundColor) }
            val textColor = contentColorFor(MaterialTheme.colorScheme.surface).toArgb()
            val textColorState = rememberSaveable { mutableIntStateOf(textColor) }

            val data = remember {
                mutableStateOf(Data(
                    uid = Uuid.random().toString(),
                    userUid = auth.uid.toString(),
                    background = backgroundColorState.intValue,
                    textColor = textColorState.intValue,
                    blocks = description.value.lineSequence().filter { it.isNotBlank() }.map { Block.paragraph(it) }.toList(),
                    medias = sharedMedias.value,
                    audios = sharedAudios.value
                ))
            }

            MainTheme(settingsModel) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = .6f))
                ) {
                    Jotting(
                        screen = screen,
                        data = data,
                        shareRevision = shareRevision.intValue,
                        sharedText = pendingSharedText.value,
                        incomingMedias = pendingSharedMedias.value,
                        incomingAudios = pendingSharedAudios.value,
                        onShareConsumed = {
                            pendingSharedText.value = null
                            pendingSharedMedias.value = emptyList()
                            pendingSharedAudios.value = emptyList()
                        }
                    ) {
                        finish()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent, publishToEditor = true)
    }

    private fun handleIncomingIntent(intent: Intent?, publishToEditor: Boolean) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> {
                val mimeType = intent.type.orEmpty()
                val uris = buildList {
                    if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
                        addAll(intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty())
                    } else {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let(::add)
                    }
                }.distinct()
                when {
                    mimeType.startsWith("text/") -> {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                        if (!sharedText.isNullOrBlank()) {
                            description.value = listOfNotNull(description.value.takeIf { it.isNotBlank() }, sharedText).joinToString("\n\n")
                            if (publishToEditor) {
                                pendingSharedText.value = sharedText
                                shareRevision.intValue++
                            }
                        }
                    }
                    mimeType.startsWith("audio/") -> {
                        val incoming = uris.map { uri ->
                            val title = uri.lastPathSegment?.substringAfterLast("/") ?: "Shared audio"
                            Audio(title = title, path = uri.toString(), uri = uri.toString())
                        }
                        sharedAudios.value = sharedAudios.value + incoming
                        if (publishToEditor && incoming.isNotEmpty()) {
                            pendingSharedAudios.value = incoming
                            shareRevision.intValue++
                        }
                    }
                    else -> {
                        val incoming = uris.map { uri ->
                            val type = MediaType.fromMime(mimeType)
                            Media(uri = uri.toString(), isVideo = type == MediaType.VIDEO, isDocument = type == MediaType.DOCUMENT, type = type, title = uri.lastPathSegment?.substringAfterLast("/"))
                        }
                        sharedMedias.value = sharedMedias.value + incoming
                        if (publishToEditor && incoming.isNotEmpty()) {
                            pendingSharedMedias.value = incoming
                            shareRevision.intValue++
                        }
                    }
                }
            }
            Intent.ACTION_PROCESS_TEXT -> {
                val sharedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
                if (!sharedText.isNullOrBlank()) {
                    description.value = sharedText
                    if (publishToEditor) {
                        pendingSharedText.value = sharedText
                        shareRevision.intValue++
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                screen.value = when {
                    intent.getBooleanExtra("quick_photo", false) -> Screens.PHOTO_SCREEN
                    intent.getBooleanExtra("quick_record", false) -> Screens.RECORD_SCREEN
                    intent.getBooleanExtra("quick_reminder", false) -> Screens.REMINDER_SCREEN
                    else -> Screens.NOTE_SCREEN
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("jotting_shared_text", description.value)
        outState.putStringArrayList("jotting_media_uris", ArrayList(sharedMedias.value.map { it.uri }))
        outState.putStringArrayList("jotting_media_types", ArrayList(sharedMedias.value.map { it.type.name }))
        outState.putStringArrayList("jotting_media_titles", ArrayList(sharedMedias.value.map { it.title.orEmpty() }))
        outState.putStringArrayList("jotting_audio_uris", ArrayList(sharedAudios.value.map { it.uri }))
        outState.putStringArrayList("jotting_audio_titles", ArrayList(sharedAudios.value.map { it.title }))
        outState.putString("jotting_screen", screen.value?.name)
    }

    private fun restoreSharedContent(state: Bundle) {
        description.value = state.getString("jotting_shared_text").orEmpty()
        val mediaTypes = state.getStringArrayList("jotting_media_types").orEmpty()
        val mediaTitles = state.getStringArrayList("jotting_media_titles").orEmpty()
        sharedMedias.value = state.getStringArrayList("jotting_media_uris").orEmpty().mapIndexed { index, uri ->
            val type = mediaTypes.getOrNull(index)
                ?.let { runCatching { MediaType.valueOf(it) }.getOrNull() }
                ?: MediaType.IMAGE
            Media(
                uri = uri,
                isVideo = type == MediaType.VIDEO,
                isDocument = type == MediaType.DOCUMENT,
                type = type,
                title = mediaTitles.getOrNull(index)
            )
        }
        val audioTitles = state.getStringArrayList("jotting_audio_titles").orEmpty()
        sharedAudios.value = state.getStringArrayList("jotting_audio_uris").orEmpty().mapIndexed { index, uri ->
            Audio(title = audioTitles.getOrNull(index).orEmpty(), path = uri, uri = uri)
        }
        screen.value = state.getString("jotting_screen")
            ?.let { runCatching { Screens.valueOf(it) }.getOrNull() }
    }

    override fun onDestroy() {
        super.onDestroy()
        intent.removeCategory(Intent.CATEGORY_DEFAULT)
        intent.action = null
        activityScope.cancel()
        playerManager.releaseController()
    }
}