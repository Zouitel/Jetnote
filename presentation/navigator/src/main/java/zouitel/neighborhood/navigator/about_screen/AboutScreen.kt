package zouitel.neighborhood.navigator.about_screen

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.navigator.R
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonTopAppBar
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.markdown.MarkdownBlock
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class AboutScreen: Screen {
    @Composable
    override fun Content() {
        About(
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel()
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun About(
        settingsModel: SettingsScreenModel,
        tagModel: TagScreenModel,
        searchModel: SearchScreenModel,
        chatModel: ChatScreenModel
    ) {
        val context = LocalContext.current
        val bottomNav = LocalBottomSheetNavigator.current
        val navigator = LocalNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val snackbarState = remember { SnackbarHostState() }
        val (lazyListState, height) = rememberAutoHeightCacheLazyList()
        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        val updateModel = koinScreenModel<UpdateScreenModel>()
        val updateState by updateModel.state.collectAsState()

        val updateLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { /* flexible flow result is delivered via the InstallStateUpdatedListener instead */ }

        LaunchedEffect(Unit) {
            updateModel.checkForUpdate()
        }

        LaunchedEffect(updateState) {
            if (updateState is UpdateUiState.ReadyToInstall) {
                snackbarState.showSnackbar("Update downloaded — tap to restart")
            }
        }

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
                        assistantModel = koinScreenModel()
                    )
                }
            ) {
                Scaffold(
                    topBar = {
                        CommonTopAppBar(
                            settingsModel = settingsModel,
                            drawerState = drawerState,
                            scrollBehavior = scrollBehavior,
                            title = stringResource(zouitel.neighborhood.logic.R.string.screen_about)
                        )
                    },
                    modifier = Modifier
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .reportable(settings.isShakable)
                        .cloudy(enabled = bottomNav.isVisible || drawerState.isOpen)
                ) { paddingValues ->
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = paddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(height)
                    ) {
                        item {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                AboutLabel()
                            }
                        }
                        item {
                            Spacer(Modifier.height(40.dp))
                            val versionLabel = buildString {
                                append("${stringResource(zouitel.neighborhood.logic.R.string.app_version)}: $appVersion")
                                if (updateState is UpdateUiState.Available) append("  •  Update available")
                                if (updateState is UpdateUiState.ReadyToInstall) append("  •  Tap to restart")
                            }

                            AboutItem(versionLabel) {
                                when (updateState) {
                                    is UpdateUiState.Available -> updateModel.startUpdate(updateLauncher, flexible = true)
                                    is UpdateUiState.ReadyToInstall -> updateModel.completeUpdate()
                                    else -> Unit
                                }
                            }
//                            AboutItem("${stringResource(zouitel.neighborhood.logic.R.string.app_version)}: $appVersion") { }

                            AboutItem(stringResource(zouitel.neighborhood.logic.R.string.term_of_use)) {
                                navigator?.push(TermsScreen())
                            }
                            AboutItem(stringResource(zouitel.neighborhood.logic.R.string.privacy_policy)) {
                                navigator?.push(PrivacyScreen())
                            }
                            AboutItem(stringResource(zouitel.neighborhood.logic.R.string.licenses)) {
                                navigator?.push(Licenses())
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AboutLabel() {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            AsyncImage(
                model = R.drawable.mat,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxWidth()
                    .blur(
                        radius = 4.dp,
                        edgeTreatment = BlurredEdgeTreatment.Rectangle
                    )
            )
            val app_title = buildAnnotatedString {
                withStyle(SpanStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.displayLarge.fontSize
                )) {
                    append("Jetnote")
                }
                withStyle(SpanStyle(
                    color = CommonColors.prime,
                    fontSize = MaterialTheme.typography.displayLarge.fontSize
                )) {
                    append(".")
                }
            }

            Text(app_title)
        }
    }

    @Composable
    private fun AboutItem(label: String, onClick: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(30.dp, 15.dp)
                )
        }
    }
}

