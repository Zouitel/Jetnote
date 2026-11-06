package zouitel.neighborhood.navigator.settings_screen

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.account.ui.SyncScreen
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreen
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.logic.LanguageManager
import zouitel.neighborhood.navigator.R
import zouitel.neighborhood.logic.R as LR
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.navigator.utils.SystemBarsBehavior
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.AnimatedCircleCheckbox
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonTopAppBar
import zouitel.neighborhood.ui.LiquidGlassSurface
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class SettingsScreen: Screen {

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    override fun Content() {
        val settingsModel = koinScreenModel<SettingsScreenModel>()
        SystemBarsBehavior(settingsModel)

        Settings(
            settingsModel,
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel(),
            koinScreenModel()
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
        val glassBackdrop = rememberLayerBackdrop()
        var blurRadius by remember(settings.liquidGlassBlurRadius) { mutableFloatStateOf(settings.liquidGlassBlurRadius) }
        var refraction by remember(settings.liquidGlassRefraction) { mutableFloatStateOf(settings.liquidGlassRefraction) }
        var tint by remember(settings.liquidGlassTint) { mutableFloatStateOf(settings.liquidGlassTint) }
        var showLanguagesSheet by remember { mutableStateOf(false) }
        val currentLang = remember { LanguageManager.getCurrentLanguage() }

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
                            title = stringResource(zouitel.neighborhood.logic.R.string.settings)
                        )
                    },
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                        .reportable(settings.isShakable)
                        .cloudy(enabled = bottomNav.isVisible || drawerState.isOpen || showLanguagesSheet),
                ) { paddingValues ->
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .layerBackdrop(glassBackdrop)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                                        )
                                    )
                                )
                        )
                        LazyColumn(
                            state = lazyListState,
                            contentPadding = paddingValues,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(height)
                        ) {
                        item {
                            NavigateItem(
                                title = stringResource(zouitel.neighborhood.logic.R.string.account_sync),
                                description = stringResource(zouitel.neighborhood.logic.R.string.account_sync_description).trimIndent(),
                                icon = {
                                    AsyncImage(
                                        R.drawable.google_drive,
                                        null
                                    )
                                }
                            ) {
                                bottomNav.show(SyncScreen())
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            NavigateItem(
                                title = stringResource(zouitel.neighborhood.logic.R.string.assistant_configuration),
                                description = stringResource(zouitel.neighborhood.logic.R.string.assistant_configuration_description).trimIndent(),
                                icon = {
                                    AsyncImage(
                                        R.drawable.star,
                                        null
                                    )
                                }
                            ) {
                                navigator.push(AssistantScreen(assistant))
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                title = stringResource(zouitel.neighborhood.logic.R.string.dark_mode),
                                description = stringResource(zouitel.neighborhood.logic.R.string.dark_mode_description).trimIndent(),
                                checked = settings.darkTheme,
                                icon = {
                                    AsyncImage(
                                        R.drawable.dark_mode,
                                        null
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                LiquidGlassSurface(
                                    backdrop = glassBackdrop,
                                    enabled = settings.liquidGlassEnabled,
                                    blurRadius = settings.liquidGlassBlurRadius,
                                    refraction = settings.liquidGlassRefraction,
                                    tintAlpha = settings.liquidGlassTint,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Liquid glass",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Customize glass surfaces across the app.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )
                                        CommonActivationItem(
                                            title = "Enable liquid glass",
                                            description = "Use translucent, blurred glass surfaces wherever the app supports them.",
                                            icon = null,
                                            checked = settings.liquidGlassEnabled,
                                            onChecked = {
                                                settingsModel.update(settings.copy(liquidGlassEnabled = it))
                                            }
                                        )
                                        Text(
                                            text = "Blur · ${blurRadius.toInt()} dp",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )
                                        Slider(
                                            value = blurRadius,
                                            onValueChange = { blurRadius = it },
                                            onValueChangeFinished = {
                                                settingsModel.update(settings.copy(liquidGlassBlurRadius = blurRadius))
                                            },
                                            valueRange = 0f..40f,
                                            enabled = settings.liquidGlassEnabled
                                        )
                                        Text(
                                            text = "Refraction · ${refraction.toInt()} dp",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Slider(
                                            value = refraction,
                                            onValueChange = { refraction = it },
                                            onValueChangeFinished = {
                                                settingsModel.update(settings.copy(liquidGlassRefraction = refraction))
                                            },
                                            valueRange = 0f..24f,
                                            enabled = settings.liquidGlassEnabled
                                        )
                                        Text(
                                            text = "Tint · ${(tint * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Slider(
                                            value = tint,
                                            onValueChange = { tint = it },
                                            onValueChangeFinished = {
                                                settingsModel.update(settings.copy(liquidGlassTint = tint))
                                            },
                                            valueRange = 0f..0.4f,
                                            enabled = settings.liquidGlassEnabled
                                        )
                                    }
                                }
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            CommonActivationItem(
                                stringResource(zouitel.neighborhood.logic.R.string.sound_effect),
                                stringResource(zouitel.neighborhood.logic.R.string.sound_effect_description).trimIndent(),
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
                                title = stringResource(zouitel.neighborhood.logic.R.string.layouts),
                                description = stringResource(zouitel.neighborhood.logic.R.string.layouts_description).trimIndent(),
                            ) {
                                bottomNav.show(LayoutScreen())
                            }
                        }
                        item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                        item {
                            SortesItem(
                                stringResource(zouitel.neighborhood.logic.R.string.sort_notes),
                                null,
                                listOf(
                                    stringResource(zouitel.neighborhood.logic.R.string.default_sort),
                                    stringResource(zouitel.neighborhood.logic.R.string.name_sort),
                                    stringResource(zouitel.neighborhood.logic.R.string.oldest_sort),
                                    stringResource(zouitel.neighborhood.logic.R.string.newest_sort),
                                    stringResource(zouitel.neighborhood.logic.R.string.priority_sort)
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
                                stringResource(zouitel.neighborhood.logic.R.string.zen_mode),
                                stringResource(zouitel.neighborhood.logic.R.string.zen_mode_description).trimIndent(),
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
                                stringResource(zouitel.neighborhood.logic.R.string.shake),
                                stringResource(zouitel.neighborhood.logic.R.string.shake_description).trimIndent(),
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
                                stringResource(zouitel.neighborhood.logic.R.string.block_screenshot),
                                stringResource(zouitel.neighborhood.logic.R.string.block_screenshot_description).trimIndent(),
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
                                title = stringResource(zouitel.neighborhood.logic.R.string.lock_mode),
                                description = stringResource(zouitel.neighborhood.logic.R.string.lock_mode_description).trimIndent(),
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
                                title = stringResource(LR.string.language),
                                description = null,
                                icon = {
                                    AsyncImage(
                                        R.drawable.translate,
                                        null
                                    )
                                }
                                ) {
                                showLanguagesSheet = true
                            }
                        }
                        item { Spacer(Modifier.padding(10.dp, 20.dp)) }
                        }
                    }

                    if (showLanguagesSheet) {
                        ModalBottomSheet({ showLanguagesSheet = false }) {
                            LazyColumn {
                                items(LanguageManager.AppLanguage.entries) { lang ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                LanguageManager.setLanguage(lang)
                                                showLanguagesSheet = false
                                            }
                                            .padding(vertical = 12.dp)
                                    ) {
//                                        RadioButton(
//                                            selected = lang == currentLang,
//                                            onClick = {
//                                                LanguageManager.setLanguage(lang)
//                                                showLanguagesSheet = false
//                                            }
//                                        )

                                        Spacer(Modifier.width(8.dp))

                                        AnimatedCircleCheckbox(
                                            lang == currentLang,
                                            MaterialTheme.colorScheme.onSurface,
                                            MaterialTheme.colorScheme.surface
                                        ) {
                                            LanguageManager.setLanguage(lang)
                                            showLanguagesSheet = false
                                        }

                                        Spacer(Modifier.width(8.dp))
                                        Text(lang.displayName)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}