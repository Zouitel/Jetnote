package zouitel.neighborhood.workspace.ui.features

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.audio.state.ItemUiState
import zouitel.neighborhood.audio.ui.list.AudioListScreenModel
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.model.Data
import kotlinx.coroutines.launch

data class AudioListScreen(val data: MutableState<Data>): Screen {
    @Composable
    override fun Content() {
        AudioList(audioListModel = koinScreenModel())
    }

    @Composable
    private fun AudioList(audioListModel: AudioListScreenModel) {
        val bottomSheetNavigator = LocalBottomSheetNavigator.current
        val navigator = LocalNavigator.current
        val uiState by remember(audioListModel, audioListModel::listUiState).collectAsState()

        LaunchedEffect(true) {
            audioListModel.updateId(data.value.uid)
                .updateBottomSheetNavigator(bottomSheetNavigator)
                .updateNavigator(navigator)
        }


        CommonBottomSheet(data.value.background) {
                LazyColumn(modifier = Modifier.animateContentSize()) {
                    items(uiState.audioFiles) {
                        AudioItem(itemState = it, audioListModel = audioListModel)
                    }
                }
            }
    }

    @Composable
    private fun AudioItem(
        itemState: ItemUiState,
        audioListModel: AudioListScreenModel
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        Surface(
            modifier = Modifier.padding(3.dp),
            color = MaterialTheme.colorScheme.onSurface,
            shape = ShapeDefaults.Large,
            tonalElevation = 0.dp,
            onClick = {
                data.value = data.value.copy(
                    audios = data.value.audios.plusElement(itemState.onClick())
                )
                scope.launch {
                    audioAmplitudes(context, itemState.onClick().uri.toUri()) { amp ->
                        data.value = data.value.copy(
                            audios = data.value.audios.minusElement(itemState.onClick())
                        )
                        data.value = data.value.copy(
                            audios = data.value.audios.plusElement(
                                itemState.onClick()
                                    .copy(
                                        amplitude = amp.map { (it * 10).toInt() }.toString()
                                        .filter { it.isDigit() })
                            )
                        )
                    }
                }
                audioListModel.updateNewAudio(false)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onSurface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    painter = painterResource(id = CommonIcons.CASSETTE),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface
                )
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = itemState.displayName,
                        color = MaterialTheme.colorScheme.surface
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = itemState.size,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }
    }
}