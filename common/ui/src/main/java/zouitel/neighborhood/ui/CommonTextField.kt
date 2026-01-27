package zouitel.neighborhood.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.content.TransferableContent
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommonTextField(
    state: TextFieldState,
    modifier: Modifier,
    placeholder: String = "...",
    textSize: TextUnit = TextUnit(19f, TextUnitType.Sp),
    textColor: Color = Color.LightGray,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActionHandler = KeyboardActionHandler {  },
    onReceive: (TransferableContent) -> Unit = {  }
) {
    Box {
        Text(
            modifier = Modifier.padding(8.dp),
            text = if (state.text.isEmpty()) placeholder else "",
            color = Color.Gray,
            fontSize = textSize,
        )

        BasicTextField(
            state = state,
            modifier = modifier
                .fillMaxWidth()
                .padding(7.dp)
                .background(Color.Transparent)
                .contentReceiver { content ->
                    onReceive(content)
                    content
                },
            textStyle = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Default,
                color = textColor
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = imeAction
            ),
            onKeyboardAction = keyboardActions
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommonTextField(
    value: String = "",
    onValueChange: (String) -> Unit = {},
    modifier: Modifier,
    placeholder: String = "...",
    enable: Boolean = true,
    textSize: TextUnit = TextUnit(19f, TextUnitType.Sp),
    textColor: Color = Color.LightGray,
    imeAction: ImeAction = ImeAction.Default,
    keyboardActions: KeyboardActions = KeyboardActions {  },
    onReceive: (TransferableContent) -> Unit = {  }
) {
    Box {
        Text(
            modifier = modifier.padding(8.dp),
            text = if (value.isEmpty()) placeholder else "",
            color = Color.Gray,
            fontSize = textSize,
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .padding(7.dp)
                .background(Color.Transparent)
                .contentReceiver { content ->
                    onReceive(content)
                    content
                },
            enabled = enable,
            textStyle = TextStyle(
                fontSize = textSize,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Default,
                color = textColor
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions
        )
    }
}