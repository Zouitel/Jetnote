package zouitel.neighborhood.audio.ui.list

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Stable
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy.KEEP
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import zouitel.neighborhood.audio.audio.AudioRepository
import zouitel.neighborhood.audio.audio.toUiState
import zouitel.neighborhood.audio.model.Audio
import zouitel.neighborhood.audio.state.ListUiState
import zouitel.neighborhood.audio.utils.AudioStorage
import zouitel.neighborhood.audio.utils.Constants.INPUT_DURATION
import zouitel.neighborhood.audio.utils.Constants.INPUT_ID
import zouitel.neighborhood.audio.utils.Constants.INPUT_TITLE
import zouitel.neighborhood.audio.utils.Constants.INPUT_UID
import zouitel.neighborhood.audio.utils.Constants.INPUT_URI
import zouitel.neighborhood.audio.worker.CopyAudioWorker
import zouitel.neighborhood.logic.withFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Stable
class AudioListScreenModel(
    application: Application,
    private val context: Context,
    private val repo: AudioRepository
) : ScreenModel {
    private val _ListUiState: MutableStateFlow<ListUiState> = MutableStateFlow(ListUiState())
    val listUiState: StateFlow<ListUiState> = _ListUiState.withFlow(ListUiState())

    private val workManager = WorkManager.getInstance(application)

    init {
        loadAudioFiles()
    }

    private fun loadAudioFiles(query: String? = null) {
        screenModelScope.launch(Dispatchers.IO) {
            _ListUiState.value = _ListUiState.value.copy(isLoadingAudios = true)
            runCatching {
                repo.loadAudioFiles(query ?: _ListUiState.value.searchQuery).map { audio ->
                    audio.toUiState {
                        val directory = AudioStorage.directory(context)
                        if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
                            throw IllegalStateException("Unable to create app audio storage directory")
                        }
                        val file = AudioStorage.destination(directory, audio.id, audio.title)

                        if (listUiState.value.newAudio && (!file.isFile || file.length() == 0L)) {
                            enqueueCopyProcessing(audio.copy(path = file.absolutePath))
                        }

                        screenModelScope.launch {
                            delay(300.milliseconds)
                            runCatching { listUiState.value.bottomSheetNavigator?.hide() }
                        }
                        audio.copy(path = file.absolutePath)
                    }
                }
            }.onSuccess { audioFiles ->
                _ListUiState.value = _ListUiState.value.copy(audioFiles = audioFiles)
            }.onFailure {
                it.printStackTrace()
            }
            _ListUiState.value = _ListUiState.value.copy(isLoadingAudios = false)
        }
    }

    fun updateId(uid: String): AudioListScreenModel {
        screenModelScope.launch { _ListUiState.value = _ListUiState.value.copy(currentId = uid) }
        return this
    }

    fun updateBottomSheetNavigator(bottomSheetNavigator: BottomSheetNavigator?): AudioListScreenModel {
        screenModelScope.launch {
            _ListUiState.value = _ListUiState.value.copy(bottomSheetNavigator = bottomSheetNavigator)
        }
        return this
    }

    fun updateNavigator(navigator: Navigator?) {
        screenModelScope.launch { _ListUiState.value = _ListUiState.value.copy(navigator = navigator) }
    }

    fun updateNewAudio(isNew: Boolean) {
        screenModelScope.launch { _ListUiState.value = _ListUiState.value.copy(newAudio = isNew) }
    }

    private fun enqueueCopyProcessing(audio: Audio) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresStorageNotLow(true)
            .build()

        val inputData = workDataOf(
            INPUT_ID to audio.id,
            INPUT_UID to audio.uid,
            INPUT_URI to audio.uri,
            INPUT_TITLE to audio.title,
            INPUT_DURATION to audio.duration
        )

        val request = OneTimeWorkRequestBuilder<CopyAudioWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork("copy_audio_${audio.id}", KEEP, request)
    }
}
