package zouitel.neighborhood.navigator.about_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.navigator.R
import zouitel.neighborhood.navigator.navigation_drawer.NavigationDrawer
import zouitel.neighborhood.navigator.utils.sound
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonConstants
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonTopAppBar
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.assistant.ui.conversation.ChatScreenModel
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.markdown.MarkdownBlock
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class AboutScreen: Screen {
    @Composable
    override fun Content() {
        About(
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
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
        val bottomNav = LocalBottomSheetNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val snackbarState = remember { SnackbarHostState() }
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
                        snackbarState = snackbarState,
                        modelsModel = getScreenModel(),
                        assistantModel = getScreenModel()
                    )
                }
            ) {
                Scaffold(
                    topBar = {
                        CommonTopAppBar(
                            settingsModel = settingsModel,
                            drawerState = drawerState,
                            scrollBehavior = scrollBehavior,
                            title = "About"
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
                        item { AboutLabel() }
                        item { AboutDescription() }
//                        item { ProjectSources(settings.isSound) }
                    }
                }
            }
        }
    }

    companion object {
        const val ABOUT = "## Your AI-Powered Universal Brain \uD83E\uDDE0⚡\n" +
                "\n" +
                "Is your digital life fragmented across a dozen different apps? Meet **Jetnote**, the next-generation productivity co-pilot designed to centralize your thoughts, actions, and intellectual resources into one seamless experience. \uD83D\uDE80\n" +
                "\n" +
                "In an increasingly complex world, Jetnote offers **universal simplicity** backed by advanced functionality. This isn’t just a digital notepad; it’s a **contextual brain** that evolves with you. \uD83E\uDDEC\n" +
                "\n" +
                "---\n" +
                "\n" +
                "### Why Jetnote? ✨\n" +
                "\n" +
                "* **Capture Everything:** From quick text snippets to media-rich entries featuring our built-in **audio waveform player** \uD83C\uDF99\uFE0F\uD83C\uDFB5.\n" +
                "* **Integrated Task Management:** Turn ideas into action with integrated checklists and smart project tags like `[project \uD83C\uDFD7\uFE0F]`.\n" +
                "* **Ironclad Security:** Work with peace of mind using our **Secure Mock Mode** to keep your most sensitive data under wraps \uD83D\uDD12\uD83D\uDEE1\uFE0F.\n" +
                "* **Cloud Sync:** Your \"brain\" stays updated across all your devices, ensuring you're always in flow ☁\uFE0F\uD83D\uDD04.\n" +
                "\n" +
                "---\n" +
                "\n" +
                "### The Power of Multi-AI Integration \uD83E\uDD16\uD83C\uDF10\n" +
                "\n" +
                "Jetnote is one of the few platforms that gives you the keys to the kingdom. We don't lock you into one ecosystem; instead, we grant you access to the world’s leading AI providers:\n" +
                "\n" +
                "* **Google Gemini** ✨\n" +
                "* **OpenAI GPT** ⚡\n" +
                "* **Anthropic Claude** \uD83C\uDF3F\n" +
                "\n" +
                "With **full assistant customization**, you are in total control of which intelligence powers your creative workflow. Whether you're drafting a heartfelt birthday wish \uD83C\uDF82 or architecting a new business strategy, Jetnote adapts to your needs.\n" +
                "\n" +
                "---\n" +
                "\n" +
                "### Experience the Future of Thought \uD83D\uDE80\n" +
                "\n" +
                "Stop searching through tabs and start building your legacy. Jetnote is universal, secure, and ready to simplify your digital landscape.\n" +
                "\n" +
                "**Download Jetnote today and unlock your universal brain.** \uD83D\uDCF2\uD83D\uDC8E"
    }

    @Composable
    private fun AboutDescription() {
        val context = LocalContext.current
        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

        MarkdownBlock(
            ABOUT,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(10.dp)
        )

        MarkdownBlock(
            "\n\n## Version: $appVersion",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(10.dp)
        )
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
}

