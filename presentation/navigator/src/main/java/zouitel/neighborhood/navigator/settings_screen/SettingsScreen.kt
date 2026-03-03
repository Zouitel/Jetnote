package zouitel.neighborhood.navigator.settings_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import zouitel.neighborhood.account.ui.SyncScreen
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.navigator.R
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.navigator.utils.SystemBarsBehavior
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants.DEFAULT_ORDER
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonConstants.NAME_ORDER
import zouitel.neighborhood.ui.CommonConstants.NEWEST_ORDER
import zouitel.neighborhood.ui.CommonConstants.OLDEST_ORDER
import zouitel.neighborhood.ui.CommonConstants.PRIORITY_ORDER
import zouitel.neighborhood.ui.CommonTopAppBar
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreen
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class SettingsScreen: Screen {

    @Composable
    override fun Content() {
        val settingsModel = getScreenModel<SettingsScreenModel>()
        SystemBarsBehavior(settingsModel)

        Settings(
            settingsModel,
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
        )
    }

    @SuppressLint(
        "UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter",
        "WrongConstant"
    )
    @Composable
    private fun Settings(
        settingsModel: SettingsScreenModel,
        tagModel: TagScreenModel,
        searchModel: SearchScreenModel,
        assistantModel: AssistantScreenModel,
        chatModel: ChatScreenModel
    ) {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val bottomNav = LocalBottomSheetNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val assistant by remember(assistantModel,assistantModel::observe).collectAsState(Assistant())
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val topAppBarState = rememberTopAppBarState()
        val snackbarState = remember { SnackbarHostState() }
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val (lazyListState, height) = rememberAutoHeightCacheLazyList()

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
                        snackbarState = snackbarState
                    )
                }
            ) {
                Scaffold(
                    topBar = {
                        CommonTopAppBar(
                            settingsModel = settingsModel,
                            drawerState = drawerState,
                            scrollBehavior = scrollBehavior,
                            title = "Settings"
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                        .reportable(settings.isShakable)
                        .cloudy(enabled = bottomNav.isVisible || drawerState.isOpen),
                ) { paddingValues ->
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = paddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(height)
                    ) {
                        item {
                            NavigateItem(
                                title = "Account Sync",
                                description = """
                                Enabling Google Drive account sync for
                                this application allows you to
                                seamlessly back up and synchronize
                                your app data, such as notes and
                                settings, this ensures that your data is safe.
                            """.trimIndent(),
                                icon = {
                                    AsyncImage(
                                        model = R.drawable.google_drive,
                                        contentDescription = "google drive"
                                    )
                                }
                            ) {
                                bottomNav.show(SyncScreen())
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            NavigateItem(
                                title = "Assistant Configuration",
                                description = """
                                AI assistant is a software program
                                powered by artificial intelligence designed
                                to help you with a wide range of tasks in
                                this application.
                                These assistants understand and respond
                                to natural language, allowing you to
                                interact with them through image or text.
                            """.trimIndent(),
                                icon = {
                                    AsyncImage(
                                        model = R.drawable.star,
                                        contentDescription = null
                                    )
                                }
                            ) {
                                navigator.push(AssistantScreen(assistant))
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                title = "Dark Mode",
                                description =  """
                                This mode is a system-wide setting that 
                                shifts the background scheme of your 
                                Android app's UI to darker colors.
                                It's designed to Reduce eye strain, improve 
                                accessibility, save battery life, and 
                                enhance user experience.
                            """.trimIndent(),
                                checked = settings.darkTheme,
                                icon = {
                                    AsyncImage(
                                        model = R.drawable.dark_mode,
                                        contentDescription = null
                                    )
                                },
                                onChecked = {
                                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                    settingsModel.update(settings.copy(darkTheme = it))
                                }
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                "Sound Effect",
                                """
                                In the context of the Jetnote application,
                                The "Sound Effect" feature provides
                                auditory feedback to users when they
                                interact with buttons and other UI
                                elements.
                                This is a common practice in
                                mobile app development to make the
                                user experience more interactive and
                                engaging.
                            """.trimIndent(),
                                null,
                                settings.isSound,
                                {
                                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                    settingsModel.update(settings.copy(isSound = it))
                                }
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            NavigateItem(
                                title = "Layouts",
                                description = """
                                Layouts are fundamental to organizing 
                                and displaying content in your Android 
                                applications.
                                They define the structure and arrangement 
                                of UI elements on the screen.
                            """.trimIndent(),
                            ) {
                                bottomNav.show(LayoutScreen())
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            SortesItem(
                                "Sort Notes By",
                                null,
                                listOf(
                                    DEFAULT_ORDER,
                                    NAME_ORDER,
                                    OLDEST_ORDER,
                                    NEWEST_ORDER,
                                    PRIORITY_ORDER
                                ),
                                settings.ordination
                            ) { order ->
                                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                settingsModel.update(settings.copy(ordination = order))
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                "Zen Mode",
                                """
                                Zen mode (Full screen mode) is a display 
                                setting in computing that allows an 
                                application to occupy the entire screen, 
                                hiding all other interface elements such as 
                                toolbars, menus, and the operating 
                                system's taskbar or menu bar. 
                            """.trimIndent(),
                                null,
                                settings.isZenMode,
                                {
                                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                    settingsModel.update(settings.copy(isZenMode = it))
                                }
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                "Shake To Report",
                                """
                              It's a convenient feature that allows users 
                              to initiate a feedback or bug reporting 
                              process simply by physically shaking their 
                              Android device. Instead of navigating 
                              through multiple menus to find a "Send 
                              Feedback" or "Report a Bug" option, 
                            """.trimIndent(),
                                null,
                                settings.isShakable,
                                {
                                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                    settingsModel.update(settings.copy(isShakable = it))
                                }
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                "Block Screenshots",
                                """
                                Screenshot blocking is a security measure 
                                that prevents users from capturing 
                                screenshots of your app's content.
                                This is often used to protect sensitive 
                                information, such as banking details, 
                                personal data, or copyrighted material.
                            """.trimIndent(),
                                null,
                                settings.isScreenshot,
                                {
                                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                    settingsModel.update(settings.copy(isScreenshot = it))
                                }
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                title = "Lock Mode",
                                description = """
                                This feature enhances the security of your 
                                app by requiring users to authenticate 
                                before they can access its content.
                                This is typically achieved using the
                                device's built-in security features, such as
                                fingerprint, PIN, or pattern unlock.
                            """.trimIndent(),
                                icon = {
                                    AsyncImage(
                                        model = R.drawable.lock,
                                        contentDescription = null
                                    )
                                },
                                checked = settings.isLocked,
                                onChecked = {
                                    sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                    settingsModel.update(settings.copy(isLocked = it))
                                }
                            )
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            NavigateItem(
                                title = "Licenses",
                                description = "Public repositories on GitHub are often used to share open source software.",
                                icon = {
                                    AsyncImage(
                                        model = R.drawable.certificate,
                                        contentDescription = null
                                    )
                                }
                            ) {
                                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                navigator.push(Licenses())
                            }
                        }
                    }
                }
            }
        }
    }
}