package zouitel.neighborhood.navigator.about_screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.chipColors
import com.mikepenz.aboutlibraries.ui.compose.libraryColors
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.navigator.R
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.webview.WebViewPageScreen

class Licenses: Screen {
    @Composable
    override fun Content() {
        Scaffold(content = { paddingValues ->
            OpenSourceContent(Modifier.padding(paddingValues), getScreenModel())
        })
    }
    @Composable
    private fun OpenSourceContent(modifier: Modifier = Modifier, settingsModel: SettingsScreenModel) {
        val navigator = LocalNavigator.current
        val bottomNav = LocalBottomSheetNavigator.current

        val libs by produceLibraries(R.raw.aboutlibraries)
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            LibrariesContainer(
                libraries = libs,
                modifier = modifier
                    .fillMaxSize()
                    .cloudy(enabled = bottomNav.isVisible)
                    .reportable(settings.isShakable),
                showDescription = true,
                showFundingBadges = true,
                onLibraryClick = {
                    navigator?.push(WebViewPageScreen(it.website ?: return@LibrariesContainer))
                },
                colors = LibraryDefaults.libraryColors(
                    libraryBackgroundColor = MaterialTheme.colorScheme.surface,
                    libraryContentColor = MaterialTheme.colorScheme.onSurface,
                    versionChipColors = LibraryDefaults.chipColors(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        CommonColors.prime
                    ),
                    licenseChipColors = LibraryDefaults.chipColors(
                        CommonColors.darkAzure,
                        CommonColors.lightAzure
                    ),
                    fundingChipColors = LibraryDefaults.chipColors(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
            )
        }
    }
}