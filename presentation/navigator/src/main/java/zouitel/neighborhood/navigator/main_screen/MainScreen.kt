package zouitel.neighborhood.navigator.main_screen

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.account.ui.SyncScreenModel
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.navigator.utils.LazyVerticalGrid
import zouitel.neighborhood.navigator.utils.LazyVerticalList
import zouitel.neighborhood.navigator.utils.SystemBarsBehavior
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.search.utils.SelectionTopBar
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList
import zouitel.neighborhood.ui.rememberAutoHeightCacheStaggeredGrid
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel

class MainScreen: Screen {

    @Composable
    override fun Content() {
        val settingsModel = getScreenModel<SettingsScreenModel>()

        SystemBarsBehavior(settingsModel)

        Main(
            getScreenModel(),
            settingsModel,
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
        )
    }

    @SuppressLint("CoroutineCreationDuringComposition", "UnusedMaterial3ScaffoldPaddingParameter")
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
        val navBottomSheet = LocalBottomSheetNavigator.current

        val syncState by remember(syncModel, syncModel::syncDataState).collectAsState()
        val isDownloading by remember(syncModel, syncModel::isDownloading).collectAsState()
        val needsInitialSync by remember(syncModel, syncModel::needsInitialSync).collectAsState()
        val uiState by remember(searchModel, searchModel::uiState).collectAsState()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val snackbarState = remember { SnackbarHostState() }
        val deletedNotes = remember { mutableStateListOf<Pair<Data, Int>>() }
        var jobCounter by remember { mutableIntStateOf(0) }
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
            stringResource(zouitel.neighborhood.logic.R.string.name_sort) -> mainModel.nameNotes
            stringResource(zouitel.neighborhood.logic.R.string.oldest_sort) -> mainModel.oldestNotes
            stringResource(zouitel.neighborhood.logic.R.string.newest_sort) -> mainModel.newestNotes
            stringResource(zouitel.neighborhood.logic.R.string.priority_sort) -> mainModel.priorityNotes
            else -> mainModel.defaultNotes
        }

        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val refreshState = rememberPullToRefreshState()
        val (lazyListState, listHeight) = rememberAutoHeightCacheLazyList()
        val (gridState, gridHeight) = rememberAutoHeightCacheStaggeredGrid()


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
                        modelsModel = getScreenModel(),
                        assistantModel = getScreenModel()
                    )
                }
            ) {
                Scaffold(
                    modifier = Modifier
                        .cloudy(enabled = navBottomSheet.isVisible || drawerState.isOpen)
                        .reportable(settings.isShakable)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        if (uiState.isSelection) {
                            SelectionTopBar(
                                searchModel = searchModel,
                                dataModel = dataModel,
                                noteAndTagModel = noteAndTagModel,
                                tagModel = tagModel
                            ) {
                                // to.do cancel the alarm manager reminder.
                                val currentJobId = jobCounter++
                                deletedNotes.add(Pair(it, currentJobId))
                                val notePair = deletedNotes.firstOrNull { note -> note.second == currentJobId }
                                dataModel.sendAction(Action.DeleteByUid(it.uid))

                                scope.launch {
                                    snackbarState.currentSnackbarData?.dismiss()
                                    val result = snackbarState.showSnackbar(
                                        message = "${notePair?.first?.title} deleted!",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )

                                    if (result == SnackbarResult.ActionPerformed) {
                                        notePair?.first?.let { note ->
                                            dataModel.sendAction(Action.Insert(note))
                                        }
                                    }
                                    notePair?.let { note -> deletedNotes.remove(note) }
                                }
                            }
                        } else {
                            MainTopAppBar(
                                settingsModel = settingsModel,
                                searchModel = searchModel,
                                scrollBehavior = scrollBehavior,
                                drawerState = drawerState,
                                syncModel = syncModel
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
                            expanded = scrollBehavior.state.collapsedFraction != 1f,
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
                            scope.launch { delay(2000) }
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