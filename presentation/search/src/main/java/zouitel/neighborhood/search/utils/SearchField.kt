package zouitel.neighborhood.search.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import zouitel.neighborhood.domain.utils.ModelConstants.DEFAULT_TXT
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.search.ui.SearchScreenModel
import zouitel.neighborhood.ui.CommonIcons.CROSS_SMALL
import zouitel.neighborhood.ui.CommonTextField

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SearchField(searchModel: SearchScreenModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester by lazy { FocusRequester() }
    val uiState by remember(searchModel, searchModel::uiState).collectAsState()

    LaunchedEffect(Unit) {
        searchModel.updateSearchTitle("")
        focusRequester.requestFocus()
    }

    Row {
        CommonTextField(
            value = uiState.text,
            onValueChange = {
                searchModel.updateSearchTitle(it)
                            },
            modifier = Modifier
                .fillMaxWidth(.8f)
                .focusRequester(focusRequester),
            placeholder = stringResource(R.string.search_in_notes),
            imeAction = ImeAction.Search,
            keyboardActions = KeyboardActions {
                keyboardController?.hide()
            }
        )
        if (uiState.text.isNotEmpty() || uiState.tag.isNotEmpty()) {
            IconButton({
                searchModel.updateSearchTitle(DEFAULT_TXT)
                searchModel.updateSearchTag(null)
                searchModel.updateSearchType(SearchTypes.Text(DEFAULT_TXT))
                keyboardController?.hide()
            }) {
                Icon(
                    painter = painterResource(CROSS_SMALL),
                    contentDescription = null
                )
            }
        }
    }
}