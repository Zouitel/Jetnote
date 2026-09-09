package zouitel.neighborhood.assistant.ui.provider

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.assistant.ui.configurations.ClaudeConfigurationScreen
import zouitel.neighborhood.assistant.ui.configurations.GlobalConfigurationScreen
import zouitel.neighborhood.assistant.ui.configurations.GoogleConfigurationScreen
import zouitel.neighborhood.assistant.utils.DEFAULT_PROVIDERS
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import com.skydoves.cloudy.cloudy
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.logic.R

class ProvidersScreen: Screen {
    @Composable
    override fun Content() {
        Providers(koinScreenModel(), koinScreenModel())
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun Providers(providerModel: ProviderScreenModel, settingsModel: SettingsScreenModel) {
        val navigator = LocalNavigator.current
        val bottomNav = LocalBottomSheetNavigator.current

        val providers by remember(providerModel, providerModel::observeAll).collectAsState()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

        Scaffold(
            topBar = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TopAppBar(
                        { Text(stringResource(R.string.providers)) },
                        Modifier,
                        {
                            IconButton({
                                navigator?.pop()
                            }) {
                                Icon(painterResource(CommonIcons.ANGLE_LEFT_20), null)
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(enabled = bottomNav.isVisible)
        ) { paddingValues ->
            LazyVerticalStaggeredGrid(
                StaggeredGridCells.Fixed(2),
                Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                items(DEFAULT_PROVIDERS) { it ->
                    ProviderCard(it) {
                        when (it) {
                            is ProviderSetting.GlobalAI -> {
                                providers.find { prov -> prov.name == it.name }
                                    ?.let { existProvider ->
                                        navigator?.push(GlobalConfigurationScreen(existProvider as ProviderSetting.GlobalAI))
                                    } ?: navigator?.push(GlobalConfigurationScreen(it))

                            }

                            is ProviderSetting.Google -> {
                                providers.find { prov -> prov.name == it.name }
                                    ?.let { existProvider ->
                                        navigator?.push(GoogleConfigurationScreen(existProvider as ProviderSetting.Google))
                                    } ?: navigator?.push(GoogleConfigurationScreen(it))
                            }

                            is ProviderSetting.Claude -> {
                                providers.find { prov -> prov.name == it.name }
                                    ?.let { existProvider ->
                                        navigator?.push(ClaudeConfigurationScreen(existProvider as ProviderSetting.Claude))
                                    } ?: navigator?.push(ClaudeConfigurationScreen(it))
                            }
                        }
                    }
                }
            }
        }
    }
}