package zouitel.neighborhood.navigator.about_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class AboutScreen: Screen {
    @Composable
    override fun Content() {
        About(
            settingsModel = getScreenModel(),
            tagModel = getScreenModel(),
            searchModel = getScreenModel()
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun About(
        settingsModel: SettingsScreenModel,
        tagModel: TagScreenModel,
        searchModel: SearchScreenModel
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
                        conversationModel = getScreenModel(),
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
        const val ABOUT = "${CommonConstants.APP_NAME} is a note-taking and task management application.\n\n" +
                " And It is intended for archiving and creating notes in which photos and tags saved and web links.\n\n" +
                "Version: ${CommonConstants.APP_VERSION}"
    }

    @Composable
    private fun AboutDescription() {
        Text(
            text = ABOUT,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
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

    @Composable
    private fun ProjectSources(isSound: Boolean?) {
        val uri = LocalUriHandler.current
        val context = LocalContext.current

        Opensource(
            text = "Project Source",
            icon = {
                Icon(
                    painter = painterResource(CommonIcons.GITHUB),
                    contentDescription = ""
                )
            }
        ) {
            sound.performSoundEffect(context, CommonConstants.KEY_CLICK, isSound)
            uri.openUri("https://github.com/City-Zouitel/JetNote")
        }
    }

    @Composable
    private fun Opensource(
        text: String,
        icon: @Composable () -> Unit,
        action: () -> Unit
    ) {
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(.5f)
                .padding(50.dp),
            onClick = action,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp)
            ) {
                icon.invoke()
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

