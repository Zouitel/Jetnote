package zouitel.neighborhood.search.utils

import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.search.ui.SearchScreen
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchTopBar(
    searchModel: SearchScreenModel,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val navigator = LocalNavigator.current
    TopAppBar(
        modifier = Modifier.clickable {
            navigator?.push(SearchScreen())
        },
        navigationIcon = {
            IconButton({
                navigator?.pop()
            }) {
                Icon(
                    painter = painterResource(CommonIcons.ARROW_LEFT),
                    contentDescription = null
                )
            }
        },
        title = {
            SearchField(searchModel)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        scrollBehavior = scrollBehavior
    )
}
