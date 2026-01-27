package zouitel.neighborhood.navigator.main_screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.account.AccountScreen
import zouitel.neighborhood.account.SyncScreen
import zouitel.neighborhood.account.SyncScreenModel
import zouitel.neighborhood.search.ui.SearchScreen
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonRow
import zouitel.neighborhood.ui.Open_Drawer
import zouitel.neighborhood.ui.SettingsScreenModel
import coil.compose.AsyncImage

@Composable
internal fun MainTopAppBar(
    settingsModel: SettingsScreenModel,
    searchModel: SearchScreenModel,
    scrollBehavior: TopAppBarScrollBehavior,
    syncModel: SyncScreenModel,
    drawerState: DrawerState
) {
    val navigator = LocalNavigator.current
    val bottomNav = LocalBottomSheetNavigator.current

    val uiState by remember(searchModel, searchModel::uiState).collectAsState()
    val user by remember(syncModel, syncModel::userState).collectAsState()

    TopAppBar(
        modifier = Modifier.clickable {
            navigator?.push(SearchScreen())
        },
        navigationIcon = {
            Row {
                AnimatedVisibility(uiState.searchTitle.isEmpty() && uiState.searchTag == null) {
                    CommonRow(Modifier.padding(start = 10.dp, end = 10.dp)) {
                        Open_Drawer(settingsModel, drawerState)
                    }
                }
            }
        },
        title = {
            Text(
                text = "Search in notes..",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        actions = {
            user?.let {
                AsyncImage(
                    model = it.photoUrl,
                    contentDescription = "account photo",
                    modifier = Modifier
                        .clip(CircleShape)
                        .sizeIn(maxHeight = 35.dp, maxWidth = 35.dp)
                        .combinedClickable(
                            onLongClick = {
                                bottomNav.show(SyncScreen())
                            }
                        ) {
                            navigator?.push(AccountScreen())
                        }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
}