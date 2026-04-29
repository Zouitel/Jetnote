package zouitel.neighborhood.navigator.about_screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.navigator.R
import zouitel.neighborhood.navigator.utils.TERMS_OF_USE
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.markdown.MarkdownBlock
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class TermsScreen: Screen {
    @Composable
    override fun Content() {
        val settingsModel: SettingsScreenModel = getScreenModel()

        val bottomNav = LocalBottomSheetNavigator.current
        val navigator = LocalNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val (lazyListState, height) = rememberAutoHeightCacheLazyList()

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Terms of Use")},
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            IconButton({
                                navigator?.pop()
                            }) {
                                Icon(painterResource(CommonIcons.ARROW_LEFT), null)
                            }
                        }
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
                        .padding(8.dp)
                ) {
                    item {
                        MarkdownBlock(TERMS_OF_USE.trimIndent())
                    }
                }
            }
        }
    }
}