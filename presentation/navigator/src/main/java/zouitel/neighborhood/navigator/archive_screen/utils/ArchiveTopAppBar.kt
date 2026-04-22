package zouitel.neighborhood.navigator.archive_screen.utils

import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.navigator.archive_screen.EraseScreen
import zouitel.neighborhood.search.ui.SearchScreen
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.Open_Drawer
import zouitel.neighborhood.ui.SettingsScreenModel

@Composable
internal fun ArchiveTopAppBar(
    settingsModel: SettingsScreenModel,
    scrollBehavior: TopAppBarScrollBehavior,
    drawerState: DrawerState
) {
    val navBottomSheet = LocalBottomSheetNavigator.current
    val navigator = LocalNavigator.current

    TopAppBar(
        navigationIcon = {
            Open_Drawer(settingsModel, drawerState)
        },
        title = {
            Text(text = stringResource(R.string.archive))
        },
        actions = {
            IconButton({
                navigator?.push(SearchScreen())
            }) {
                Icon(
                    painter = painterResource(CommonIcons.SEARCH),
                    contentDescription = null
                )
            }
            IconButton(
                onClick = {
                    /*homeModel.updateErasing(true)*/
                    navBottomSheet.show(EraseScreen {})
                }
            ) {
                Icon(
                    painterResource(CommonIcons.BROOM),
                    null
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
}