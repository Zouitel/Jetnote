package zouitel.neighborhood.audio.state

import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.BottomSheetNavigator
import zouitel.neighborhood.audio.model.Audio

data class ListUiState(
    val searchQuery: String = "",
    val audioFiles: List<ItemUiState> = emptyList(),
    val isLoadingAudios: Boolean = false,
    val currentId: String = "",
    val bottomSheetNavigator: BottomSheetNavigator? = null,
    val navigator: Navigator? = null,
    val newAudio: Boolean = true,
    val clickedAudio: Audio? = null
)