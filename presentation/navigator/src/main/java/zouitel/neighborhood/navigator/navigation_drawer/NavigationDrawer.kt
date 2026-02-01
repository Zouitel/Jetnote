package zouitel.neighborhood.navigator.navigation_drawer

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import zouitel.neighborhood.assistant.model.Conversation
import zouitel.neighborhood.assistant.ui.conversation.ChatScreen
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.workspace.provider.SharedScreen
import zouitel.neighborhood.logic.sharApp
import zouitel.neighborhood.navigator.about_screen.AboutScreen
import zouitel.neighborhood.navigator.archive_screen.ArchiveScreen
import zouitel.neighborhood.navigator.settings_screen.SettingsScreen
import zouitel.neighborhood.navigator.tags_screen.HashTagsScreen
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.search.ui.SearchScreen
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.search.utils.AppScreen
import zouitel.neighborhood.search.utils.SearchTypes
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonIcons.COMMENT_EXCLAMATION
import zouitel.neighborhood.ui.CommonIcons.CROSS_SMALL
import zouitel.neighborhood.ui.CommonIcons.HOME
import zouitel.neighborhood.ui.CommonIcons.INBOX
import zouitel.neighborhood.ui.CommonIcons.INTERROGATION
import zouitel.neighborhood.ui.CommonIcons.SETTINGS
import zouitel.neighborhood.ui.CommonIcons.SHARE
import zouitel.neighborhood.ui.CommonIcons.SPARKLES
import zouitel.neighborhood.ui.CommonIcons.TAGS
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.tag.ui.TagScreenModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun NavigationDrawer(
    settingsModel: SettingsScreenModel,
    tagModel: TagScreenModel,
    searchModel: SearchScreenModel,
    conversationModel: ChatScreenModel,
    drawerState: DrawerState,
    snackbarState: SnackbarHostState
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow

    val tags by remember(tagModel, tagModel::observeAll).collectAsState()
    val settings by remember(settingsModel, settingsModel::observe).collectAsState()
    val conversations by remember(conversationModel, conversationModel::observeAll).collectAsState()
    val uiState by remember(searchModel, searchModel::uiState).collectAsState()
    val scope = rememberCoroutineScope()
    var tagsMaxLines by remember { mutableIntStateOf(3) }
    var conversationsMaxLines by remember { mutableIntStateOf(3) }
    val jobCounter = remember { mutableIntStateOf(0) }
    val deletedChats = remember { mutableStateListOf<Pair<Conversation, Int>>() }
    val deletedTags = remember { mutableStateListOf<Pair<Tag, Int>>() }
    val (lazyListState, height) = rememberAutoHeightCacheLazyList()

    // Get current screen from navigator and map to AppScreen
    val currentScreenFromNavigator by remember {
        derivedStateOf {
            when (navigator.lastItem::class.simpleName) {
                "MainScreen" -> AppScreen.MAIN_SCREEN
                "AssistantScreen" -> AppScreen.CHAT_SCREEN
                "HashTagsScreen" -> AppScreen.TAGS_SCREEN
                "SettingsScreen" -> AppScreen.SETTINGS_SCREEN
                "WidgetScreen" -> AppScreen.WIDGETS_SCREEN
                "ArchiveScreen" -> AppScreen.ARCHIVE_SCREEN
                "AboutScreen" -> AppScreen.ABOUT_SCREEN
                "FeedbackScreen" -> AppScreen.FEEDBACK_AND_HELP
                else -> uiState.currentScreen
            }
        }
    }

    // Update searchModel when navigator changes
    LaunchedEffect(currentScreenFromNavigator) {
        if (currentScreenFromNavigator != uiState.currentScreen) {
            searchModel.updateCurrentScreen(currentScreenFromNavigator)
        }
    }

    val mainScreen = rememberScreen(SharedScreen.MainScreen)
    val feedbackScreen = rememberScreen(SharedScreen.FeedbackScreen)
//    val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

    DismissibleDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.padding(end = 100.dp),
        drawerShape = RoundedCornerShape(topEnd = 15.dp, bottomEnd = 15.dp)
    ) {
        LazyColumn(state = lazyListState, modifier = Modifier.then(height)) {
            item {
                val app_title = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                    )) {
                        append("Jetnote")
                    }
                    withStyle(SpanStyle(
                        color = CommonColors.prime,
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                    )) {
                        append(".")
                    }
                }
                Text(
                    text = app_title,
                    modifier = Modifier.padding(20.dp)
                )
            }

            item { HorizontalDivider() }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = AppScreen.MAIN_SCREEN.screen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(HOME), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.MAIN_SCREEN) {
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.MAIN_SCREEN)
                                navigator.push(mainScreen)
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.MAIN_SCREEN,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }
            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = AppScreen.CHAT_SCREEN.screen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(SPARKLES), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.CHAT_SCREEN) {
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.CHAT_SCREEN)
                                navigator.push(ChatScreen(true))
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.CHAT_SCREEN,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }

            item {
                ContextualFlowRow(
                    modifier = Modifier.animateContentSize().padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    itemCount = conversations.size,
                    maxLines = conversationsMaxLines,
                    overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            ElevatedFilterChip(
                                selected = true,
                                shape = CircleShape,
                                onClick = { conversationsMaxLines += 3 },
                                label = {
                                    Text(
                                        text = "+ more",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CommonColors.prime
                                )
                            )
                        },
                        collapseIndicator = {
                            if (conversationsMaxLines > 3) {
                                ElevatedFilterChip(
                                    selected = true,
                                    shape = CircleShape,
                                    onClick = { conversationsMaxLines = 3 },
                                    label = {
                                        Icon(
                                            modifier = Modifier.size(15.dp),
                                            painter = painterResource(CROSS_SMALL),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                )
                            }
                        }
                    )
                ) { index ->
                    ElevatedFilterChip(
                        selected = false,
                        onClick = {
                            scope.launch {
                                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                navigator.push(
                                    ChatScreen(
                                        false,
                                        conversations.getOrNull(index)?.id ?: return@launch
                                    )
                                )
                                drawerState.close()
                            }
                        },
                        label = {
                            conversations.getOrNull(index)?.title.let {
                                Text(
                                    text = it ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    modifier = Modifier.combinedClickable(
                                        onLongClick = {
                                            val currentJobId = jobCounter.intValue++
                                            deletedChats.add(
                                                Pair(
                                                    conversations.getOrNull(index)
                                                        ?: return@combinedClickable,
                                                    currentJobId
                                                )
                                            )
                                            val chatPair =
                                                deletedChats.firstOrNull { pair -> pair.second == currentJobId }

                                            scope.launch {
                                                conversationModel.upsert(
                                                    conversations.getOrNull(index)?.copy(
                                                        id = chatPair?.first?.id
                                                            ?: return@launch,
                                                        title = "",
                                                        nodes = emptyList(),
                                                        suggestions = emptyList()
                                                    ) ?: return@launch
                                                )
                                            }

                                            scope.launch {
                                                snackbarState.currentSnackbarData?.dismiss()
                                                val result = snackbarState.showSnackbar(
                                                    message = "${chatPair?.first?.title} deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Long
                                                )

                                                if (result == SnackbarResult.ActionPerformed) {
                                                    chatPair?.first?.let { chat ->
                                                        conversationModel.upsert(chat)
                                                    }
                                                }
                                                chatPair.let { chat -> deletedChats.remove(chat) }
                                            }
                                        },
                                        onClick = {
                                            scope.launch {
                                                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                                navigator.push(
                                                    ChatScreen(
                                                        false,
                                                        conversations.getOrNull(index)?.id ?: return@launch
                                                    )
                                                )
                                                drawerState.close()
                                            }
                                        }
                                    )
                                )
                            }
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            Color(MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()).copy(
                                alpha = .4f
                            ),
                            Color(MaterialTheme.colorScheme.onSurface.toArgb()),
                            Color(MaterialTheme.colorScheme.onSurface.toArgb())
                        )
                    )
                    Spacer(Modifier.width(2.dp))
                }
            }

            item { HorizontalDivider() }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = AppScreen.TAGS_SCREEN.screen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(TAGS), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.TAGS_SCREEN) {
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.TAGS_SCREEN)
                                navigator.push(HashTagsScreen())
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.TAGS_SCREEN,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }

            item {
                ContextualFlowRow(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    verticalArrangement = Arrangement.Center,
                    itemCount = tags.size,
                    maxLines = tagsMaxLines,
                    overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
                        expandIndicator = {
                            ElevatedFilterChip(
                                selected = true,
                                shape = CircleShape,
                                onClick = { tagsMaxLines += 3 },
                                label = {
                                    Text(
                                        text = "+ more",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.surface
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CommonColors.prime
                                )
                            )
                        },
                        collapseIndicator = {
                            if (tagsMaxLines > 3) {
                                ElevatedFilterChip(
                                    selected = true,
                                    shape = CircleShape,
                                    onClick = { tagsMaxLines = 3 },
                                    label = {
                                        Icon(
                                            modifier = Modifier.size(15.dp),
                                            painter = painterResource(CROSS_SMALL),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                )
                            }
                        }
                    )
                ) { index ->
                    ElevatedFilterChip(
                        selected = false,
                        onClick = {
                            scope.launch {
                                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                navigator.push(
                                    SearchScreen(
                                        SearchTypes.Tag(
                                            tags.getOrNull(index)?.label ?: return@launch
                                        )
                                    )
                                )
                                drawerState.close()
                            }
                        },
                        label = {
                            tags.getOrNull(index)?.label.let {
                                Text(
                                    text = it ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    modifier = Modifier.combinedClickable(
                                        onLongClick = {
                                            val currentJobId = jobCounter.intValue++
                                            deletedTags.add(
                                                Pair(
                                                    tags.getOrNull(index)
                                                        ?: return@combinedClickable,
                                                    currentJobId
                                                )
                                            )
                                            val tagPair = deletedTags.firstOrNull { pair -> pair.second == currentJobId }
                                            tagModel.sendAction(
                                                Action.DeleteById(tags.getOrNull(index)?.id ?: return@combinedClickable)
                                            )

                                            scope.launch {
                                                snackbarState.currentSnackbarData?.dismiss()
                                                val result = snackbarState.showSnackbar(
                                                    message = "${tagPair?.first?.label} deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Long
                                                )

                                                if (result == SnackbarResult.ActionPerformed) {
                                                    tagPair?.first.let { tag ->
                                                        tagModel.sendAction(Action.Insert(tag))
                                                    }
                                                }
                                                tagPair.let { tag -> deletedTags.remove(tag) }
                                            }
                                        },
                                        onClick = {
                                            scope.launch {
                                                sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                                                navigator.push(
                                                    SearchScreen(
                                                        SearchTypes.Tag(
                                                            tags.getOrNull(index)?.label ?: return@launch
                                                        )
                                                    )
                                                )
                                                drawerState.close()
                                            }
                                        }
                                    )
                                )
                            }
                        },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            Color(
                                tags.getOrNull(index)?.color
                                    ?: MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
                            ).copy(alpha = .4f),
                            Color(
                                tags.getOrNull(index)?.color
                                    ?: MaterialTheme.colorScheme.onSurface.toArgb()
                            ),
                            Color(
                                tags.getOrNull(index)?.color
                                    ?: MaterialTheme.colorScheme.onSurface.toArgb()
                            )
                        )
                    )
                    Spacer(Modifier.width(2.dp))
                }
            }

            item { HorizontalDivider() }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = AppScreen.SETTINGS_SCREEN.screen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(SETTINGS), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.SETTINGS_SCREEN) {
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.SETTINGS_SCREEN)
                                navigator.push(SettingsScreen())
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.SETTINGS_SCREEN,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }

//            item {
//                NavigationDrawerItem(
//                    label = {
//                        Text(
//                            text = AppScreen.WIDGETS_SCREEN.screen,
//                            style = MaterialTheme.typography.titleLarge
//                        )
//                    },
//                    icon = { Icon(painterResource(APPS_ADD), null) },
//                    selected = uiState.currentScreen == AppScreen.WIDGETS_SCREEN,
//                    onClick = {
//                        if (uiState.currentScreen != AppScreen.WIDGETS_SCREEN) {
//                            scope.launch {
//                                searchModel.updateCurrentScreen(AppScreen.WIDGETS_SCREEN)
//                                navigator.push(WidgetScreen())
//                                drawerState.close()
//                            }
//                        } else {
//                            scope.launch { drawerState.close() }
//                        }
//                    }
//                )
//            }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = AppScreen.ARCHIVE_SCREEN.screen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(INBOX), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.ARCHIVE_SCREEN) {
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.ARCHIVE_SCREEN)
                                navigator.push(ArchiveScreen())
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.ARCHIVE_SCREEN,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }

            item { HorizontalDivider() }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Share This App",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(SHARE), null) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            sharApp(context, "[COMING SOON.]")
                        }
                    }
                )
            }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "Feedback",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(COMMENT_EXCLAMATION), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.FEEDBACK_AND_HELP) {
                            sound.performSoundEffect(context, KEY_CLICK, settings.isSound)
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.FEEDBACK_AND_HELP)
                                navigator.push(feedbackScreen)
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.FEEDBACK_AND_HELP,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }

            item {
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = AppScreen.ABOUT_SCREEN.screen,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    icon = { Icon(painterResource(INTERROGATION), null) },
                    onClick = {
                        if (uiState.currentScreen != AppScreen.ABOUT_SCREEN) {
                            scope.launch {
                                searchModel.updateCurrentScreen(AppScreen.ABOUT_SCREEN)
                                navigator.push(AboutScreen())
                                delay(100)
                                drawerState.close()
                            }
                        } else {
                            scope.launch { drawerState.close() }
                        }
                    },
                    selected = uiState.currentScreen == AppScreen.ABOUT_SCREEN,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.surface,
                        selectedTextColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = CommonColors.prime,
                    )
                )
            }
        }
    }
}