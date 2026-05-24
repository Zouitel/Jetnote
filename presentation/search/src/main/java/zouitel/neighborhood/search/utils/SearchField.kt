package zouitel.neighborhood.search.utils

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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

/**
 * Inline search field (used in legacy non-SearchBar top-bar layouts).
 *
 * All text is read from / written to [searchModel.fieldState] — the single
 * stable [TextFieldState] on the ScreenModel.  It is NEVER read from UiState,
 * which no longer holds a fieldState reference.
 */
@Composable
internal fun SearchField(searchModel: SearchScreenModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester     = remember { FocusRequester() }
    val uiState            by remember(searchModel, searchModel::uiState).collectAsState()

    // True when there is content to clear (field text OR active tag filter).
    val hasClearableContent by remember {
        derivedStateOf {
            searchModel.fieldState.text.isNotBlank() || uiState.tag.isNotEmpty()
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row {
        CommonTextField(
            value         = searchModel.fieldState.text.toString(),
            onValueChange = { searchModel.setFieldText(it) },
            modifier      = Modifier
                .fillMaxWidth(0.8f)
                .focusRequester(focusRequester),
            placeholder     = stringResource(R.string.search_in_notes),
            imeAction       = ImeAction.Search,
            keyboardActions = KeyboardActions { keyboardController?.hide() },
        )

        if (hasClearableContent) {
            IconButton(onClick = {
                searchModel.clearSearch()
                keyboardController?.hide()
            }) {
                Icon(painterResource(CROSS_SMALL), contentDescription = null)
            }
        }
    }
}