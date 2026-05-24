package zouitel.neighborhood.search.utils

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.SettingsScreenModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.search.ui.Search
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    scrollBehavior: SearchBarScrollBehavior,
    searchModel: SearchScreenModel,
    settingsModel: SettingsScreenModel,
    tagModel: TagScreenModel,
    audioModel: AudioScreenModel,
    linkModel: LinkScreenModel,
    dataModel: DataScreenModel,
    noteAndTagModel: NoteAndTagScreenModel,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchbarState = rememberContainedSearchBarState()
    val scope = rememberCoroutineScope()
    val uiState by remember(searchModel, searchModel::uiState).collectAsState()

    val hasClearableContent by remember {
        derivedStateOf {
            searchModel.fieldState.text.isNotBlank() ||
                    uiState.tag.isNotEmpty() ||
                    uiState.typeState !is SearchTypes.Text
        }
    }

    val inputField: @Composable () -> Unit = {
        SearchBarDefaults.InputField(
            textFieldState = searchModel.fieldState,
            searchBarState = searchbarState,
            onSearch = { keyboardController?.hide() },
            placeholder = {
                Text(
                    modifier = Modifier.clearAndSetSemantics {},
                    text = "Search notes…"
                )
            },
            leadingIcon = {
                if (searchbarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(
                        onClick = {
                            scope.launch { searchbarState.animateToCollapsed() }
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            painterResource(CommonIcons.ARROW_LEFT),
                            null
                        )
                    }
                }
            },
            trailingIcon = {
                if (hasClearableContent) {
                    IconButton(onClick = {
                        searchModel.clearSearch()
                    }) {
                        Icon(
                            painterResource(CommonIcons.CROSS_SMALL),
                            null
                        )
                    }
                }
            }
        )
    }

    AppBarWithSearch(
        scrollBehavior = scrollBehavior,
        state = searchbarState,
        inputField = inputField,
        navigationIcon = navigationIcon,
        actions = actions
    )

    ExpandedFullScreenContainedSearchBar(
        state = searchbarState,
        inputField = inputField,
    ) {
        Search(
            null,
            searchModel = searchModel,
            settingsModel = settingsModel,
            tagModel = tagModel,
            audioModel = audioModel,
            linkModel = linkModel,
            dataModel = dataModel,
            noteAndTagModel = noteAndTagModel
        )
    }
}