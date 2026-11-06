package zouitel.neighborhood.navigator.main_screen

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import zouitel.neighborhood.account.ui.AccountScreen
import zouitel.neighborhood.account.ui.SyncScreen
import zouitel.neighborhood.account.ui.SyncScreenModel
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.navigator.utils.LazyVerticalGrid
import zouitel.neighborhood.navigator.utils.LazyVerticalList
import zouitel.neighborhood.navigator.utils.SystemBarsBehavior
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.search.utils.SearchTopBar
import zouitel.neighborhood.search.utils.SelectionTopBar
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.OpenDrawer
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel

@RequiresApi(Build.VERSION_CODES.S)
class MainScreen: Screen {
    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsScreenModel>()
        SystemBarsBehavior(settingsModel)

        Main(
            koinScreenModel(),
            settingsModel,
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel()
        )
    }

    @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun Main(
        mainModel: MainScreenModel,
        settingsModel: SettingsScreenModel,
        tagModel: TagScreenModel,
        noteAndTagModel: NoteAndTagScreenModel,
        dataModel: DataScreenModel,
        audioModel: AudioScreenModel,
        linkModel: LinkScreenModel,
        searchModel: SearchScreenModel,
        syncModel: SyncScreenModel,
        chatModel: ChatScreenModel
    ) {

        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val navBottom = LocalBottomSheetNavigator.current

        val syncState by remember(syncModel, syncModel::syncDataState).collectAsState()
        val isDownloading by remember(syncModel, syncModel::isDownloading).collectAsState()
        val needsInitialSync by remember(syncModel, syncModel::needsInitialSync).collectAsState()
        val uiState by remember(searchModel, searchModel::uiState).collectAsState()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val snackbarState = remember { SnackbarHostState() }
        var searchExpandRequest by remember { mutableStateOf<Any?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(isDownloading, syncState) {
            // When downloading stops
            if (!isDownloading && !syncState && needsInitialSync) {
                snackbarState.showSnackbar(
                    message = "Sync complete!",
                    duration = SnackbarDuration.Short
                )
                // Reset flag
                syncModel.resetInitialSyncFlag()
            }
        }

        // to observer notes while changing immediately.
        BackHandler(
            enabled = !uiState.isSelection
        ) {
            scope.launch {
                if (drawerState.isOpen) drawerState.close()
                else navigator.pop()
            }
        }

        val notes = when (settings.ordination) {
            stringResource(R.string.name_sort) -> mainModel.nameNotes
            stringResource(R.string.oldest_sort) -> mainModel.oldestNotes
            stringResource(R.string.newest_sort) -> mainModel.newestNotes
            stringResource(R.string.priority_sort) -> mainModel.priorityNotes
            else -> mainModel.defaultNotes
        }

        val searchBarBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

        val refreshState = rememberPullToRefreshState()
        val user by remember(syncModel, syncModel::userState).collectAsState()

        val workplaceScreen = rememberScreen(
            SharedScreen.Workplace(Data(),true)
        )

        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarState) { data ->
                    Snackbar(
                        snackbarData = data,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = CommonColors.prime
                    )
                }
            }
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    NavigationDrawer(
                        settingsModel = settingsModel,
                        tagModel = tagModel,
                        chatModel = chatModel,
                        searchModel = searchModel,
                        drawerState = drawerState,
                        snackbarState = snackbarState,
                        modelsModel = koinScreenModel(),
                        assistantModel = koinScreenModel(),
                        onOpenSearch = { searchExpandRequest = Any() }
                    )
                }
            ) {
                Scaffold(
                    modifier = Modifier
                        .cloudy(enabled = navBottom.isVisible || drawerState.isOpen)
                        .reportable(settings.isShakable)
                        .nestedScroll(searchBarBehavior.nestedScrollConnection),
                    topBar = {
                        if (uiState.isSelection) {
                            SelectionTopBar(
                                searchModel = searchModel,
                                dataModel = dataModel,
                                noteAndTagModel = noteAndTagModel,
                                tagModel = tagModel
                            ) {
                                val notesToDelete = it.distinctBy(Data::uid)
                                notesToDelete.forEach { note ->
                                    dataModel.sendAction(Action.DeleteByUid(note.uid))
                                }

                                scope.launch {
                                    snackbarState.currentSnackbarData?.dismiss()
                                    val message = if (notesToDelete.size == 1) {
                                        "${notesToDelete.single().title} deleted!"
                                    } else {
                                        "${notesToDelete.size} notes deleted!"
                                    }
                                    val result = snackbarState.showSnackbar(
                                        message = message,
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )

                                    if (result == SnackbarResult.ActionPerformed) {
                                        notesToDelete.forEach { note ->
                                            dataModel.sendAction(Action.Insert(note))
                                        }
                                    }
                                }
                            }
                        } else {
                            SearchTopBar(
                                scrollBehavior = searchBarBehavior,
                                searchModel = searchModel,
                                settingsModel = settingsModel,
                                tagModel = tagModel,
                                audioModel = audioModel,
                                linkModel = linkModel,
                                dataModel = dataModel,
                                noteAndTagModel = noteAndTagModel,
                                expandRequestKey = searchExpandRequest,
                                navigationIcon = {
                                    OpenDrawer(settingsModel, drawerState)
                                },
                                actions = {
                                    user?.let {
                                        Row {
                                            AsyncImage(
                                                model = it.photoUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .sizeIn(maxHeight = 35.dp, maxWidth = 35.dp)
                                                    .combinedClickable(
                                                        onLongClick = {
                                                            navBottom.show(SyncScreen())
                                                        }
                                                    ) {
                                                        navigator.push(AccountScreen())
                                                    }
                                            )
                                            Spacer(Modifier.width(10.dp))
                                        }
                                    }
                                }
                            )
                        }
                    },
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            text = {
                                Text(
                                    text = "Compose",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            icon = {
                                Icon(painterResource(CommonIcons.PLUS_20), null)
                            },
                            onClick = {
                                sound.performSoundEffect(
                                    context,
                                    CommonConstants.KEY_STANDARD,
                                    settings.isSound
                                )
                                navigator.push(workplaceScreen)
                            },
                            expanded = searchBarBehavior.scrollOffset == 0f,
                            containerColor = CommonColors.prime,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    }
                ) { paddingValues ->
                    PullToRefreshBox(
                        isRefreshing = syncState,
                        onRefresh = {
                            syncModel.enqueueSyncProcessing()
                            syncModel.enqueueDownloadsProcessing()
                        },
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        state = refreshState,
                        indicator = {
                            LoadingIndicator(
                                refreshState,
                                syncState,
                                containerColor = CommonColors.prime,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    ) {
                        if (settings.layout == CommonConstants.LIST) {
                            LazyVerticalList(
                                mainModel,
                                notes,
                                settingsModel,
                                audioModel,
                                linkModel,
                                searchModel
                            )
                        } else {
                            LazyVerticalGrid(
                                mainModel,
                                notes,
                                settingsModel,
                                audioModel,
                                linkModel,
                                searchModel
                            )
                        }
                    }
                }
            }
        }
    }
}