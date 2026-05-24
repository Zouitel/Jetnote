package zouitel.neighborhood.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTopAppBar(
    settingsModel: SettingsScreenModel,
    drawerState: DrawerState,
    scrollBehavior: TopAppBarScrollBehavior,
    title: String
) {
    TopAppBar(
        navigationIcon = {
            Row {
                CommonRow(
                    Modifier.padding(start = 10.dp, end = 10.dp),
                ) {
                    OpenDrawer(settingsModel, drawerState = drawerState)
                }
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 15.dp)
            )
        },
        scrollBehavior = scrollBehavior
    )
}