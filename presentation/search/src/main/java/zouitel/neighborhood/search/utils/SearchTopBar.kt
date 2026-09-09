package zouitel.neighborhood.search.utils

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.SettingsScreenModel
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.logic.R
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
    actions: @Composable (RowScope.() -> Unit)? = null,
    expandRequestKey: Any? = null,
    onCollapsed: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchbarState = rememberContainedSearchBarState()
    val scope = rememberCoroutineScope()
    val uiState by remember(searchModel, searchModel::uiState).collectAsState()

    // Callers that open this bar from an icon button or drawer item (rather than
    // tapping the persistent inline field) drive this by changing expandRequestKey
    // to a new, distinct value each time — e.g. Any() per click, or true on a
    // composable that gets freshly mounted per open. A stable/unchanged key won't
    // re-fire, since ArchiveScreen's SearchTopBar is only mounted while searching
    // anyway; MainScreen's is mounted permanently so it needs a genuinely new key
    // per request to expand again after the user has collapsed it.
    LaunchedEffect(expandRequestKey) {
        if (expandRequestKey != null) searchbarState.animateToExpanded()
    }

    // Report back once the bar returns to Collapsed (user tapped the back arrow,
    // or the system back gesture) so the caller can swap its own top bar back in.
    // The initial value is already Collapsed, so drop(1) skips that first emission.
    LaunchedEffect(searchbarState) {
        snapshotFlow { searchbarState.currentValue }
            .drop(1)
            .collect { value ->
                if (value == SearchBarValue.Collapsed) onCollapsed()
            }
    }

    val hasClearableContent by remember {
        derivedStateOf {
            searchModel.fieldState.text.isNotBlank() ||
                    uiState.tag.isNotEmpty() ||
                    uiState.typeState !is SearchTypes.Text
        }
    }

    val inputField: @Composable () -> Unit = {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            SearchBarDefaults.InputField(
                textFieldState = searchModel.fieldState,
                searchBarState = searchbarState,
                onSearch = { keyboardController?.hide() },
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_in_notes),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 12.sp,
                            maxFontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            stepSize = 1.sp
                        )
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
                                painterResource(CommonIcons.ANGLE_LEFT_20),
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