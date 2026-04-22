package zouitel.neighborhood.navigator.archive_screen

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.mutableStateStackOf
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.audio.ui.audio.AudioScreenModel
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.navigator.archive_screen.utils.ArchiveTopAppBar
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.search.note_card.NoteCard
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.search.utils.SelectionTopBar
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.ui.workplace.DataScreenModel
import com.google.firebase.auth.FirebaseAuth
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class ArchiveScreen: Screen {
    @Composable
    override fun Content() {
        Archive(
            getScreenModel(),
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

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun Archive(
        settingsModel: SettingsScreenModel,
        archiveModel: ArchiveScreenModel,
        audioModel: AudioScreenModel,
        linkModel: LinkScreenModel,
        searchModel: SearchScreenModel,
        dataModel: DataScreenModel,
        tagModel: TagScreenModel,
        noteAndTagModel: NoteAndTagScreenModel,
        chatModel: ChatScreenModel
    ) {
        val bottomNav = LocalBottomSheetNavigator.current
        val auth = FirebaseAuth.getInstance()

        LaunchedEffect(true) {
            archiveModel.initializeUser(auth.currentUser?.uid.toString())
        }

        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val archives = remember(archiveModel, archiveModel::observeArchives).collectAsState()
        val uiState by remember(searchModel, searchModel::uiState).collectAsState()
        val gridLayoutState = rememberLazyStaggeredGridState()
        val scope = rememberCoroutineScope()
        val (lazyListState, height) = rememberAutoHeightCacheLazyList()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val deletedNote = remember { mutableStateStackOf<Data>() }
        val snackbarState = remember { SnackbarHostState() }

        val undoResource = stringResource(R.string.action_undo)
        val isDeletedResource = stringResource(R.string.deleted)

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
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .reportable(settings.isShakable)
                        .cloudy(enabled = bottomNav.isVisible || drawerState.isOpen),
                    topBar = {
                        if(uiState.isSelection) {
                            SelectionTopBar(
                                searchModel = searchModel,
                                dataModel = dataModel,
                                noteAndTagModel = noteAndTagModel,
                                tagModel = tagModel
                            ) {
                                // to.do cancel the alarm manager reminder.
                                deletedNote.push(it)
                                dataModel.sendAction(Action.DeleteByUid(deletedNote.lastItemOrNull?.uid ?: return@SelectionTopBar))

                                scope.launch {
                                    val snackbarResult = snackbarState.showSnackbar(
                                        message = "${deletedNote.lastItemOrNull?.title} $isDeletedResource",
                                        actionLabel = undoResource
                                    )
                                    if (snackbarResult == SnackbarResult.ActionPerformed) {
                                        dataModel.sendAction(Action.Insert(deletedNote.lastItemOrNull ?: return@launch))
                                        deletedNote.pop()
                                    } else {
                                        deletedNote.pop()
                                    }
                                }
                            }
                        } else {
                            ArchiveTopAppBar(
                                settingsModel = settingsModel,
                                scrollBehavior = scrollBehavior,
                                drawerState = drawerState
                            )
                        }
                    }
                ) { paddingValues ->
                    Box(
                        contentAlignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (settings.ordination == CommonConstants.LIST) {
                            LazyColumn(
                                state = lazyListState,
                                contentPadding = paddingValues,
                                modifier = Modifier
                                    .animateContentSize()
                                    .fillMaxSize()
                                    .then(height),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(
                                    items = archives.value,
                                    key = { it.data.uid }
                                ) { entity ->
                                    NoteCard(
                                        settingsModel = settingsModel,
                                        audioModel = audioModel,
                                        linkModel = linkModel,
                                        searchModel = searchModel,
                                        note = entity
                                    )
                                }
                            }
                        } else {
                            LazyVerticalStaggeredGrid(
                                state = gridLayoutState,
                                contentPadding = paddingValues,
                                modifier = Modifier
                                    .animateContentSize()
                                    .fillMaxSize(),
                                columns = StaggeredGridCells.Fixed(2),
                                verticalItemSpacing = 6.dp,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(
                                    items = archives.value,
                                    key = { it.data.uid }
                                ) { entity ->
                                    NoteCard(
                                        settingsModel = settingsModel,
                                        audioModel = audioModel,
                                        linkModel = linkModel,
                                        searchModel = searchModel,
                                        note = entity
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}