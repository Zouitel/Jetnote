package zouitel.neighborhood.search.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonIcons.CROSS_SMALL

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SelectionsCount(searchModel: SearchScreenModel) {
    val uiState by remember(searchModel, searchModel::uiState).collectAsState()

    IconButton({
        searchModel.updateSelection(false)
        searchModel.clearSelectionNotes()
    }) {
        Icon(
            painter = painterResource(CROSS_SMALL),
            contentDescription = null,
        )
    }
    // number of selected notes.
    Text(
        text = uiState.selectedNotes.count().toString(),
        fontSize = 24.sp,
        modifier = Modifier.padding(5.dp)
    )
}