package zouitel.neighborhood.workspace.ui.bottom_bar

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import zouitel.neighborhood.ui.CommonConstants.KEY_CLICK
import zouitel.neighborhood.ui.CommonIcons.REDO
import zouitel.neighborhood.ui.CommonIcons.UNDO
import zouitel.neighborhood.ui.SoundEffect

// TODO: need improvement.
@Composable
fun UndoRedo(
    isSound: Boolean?,
    titleState: TextFieldState?,
    descriptionState: TextFieldState?
) {
    val context = LocalContext.current
    val sound = SoundEffect()

    IconButton(
        onClick = {
            sound.performSoundEffect(context, KEY_CLICK, isSound)
            if (titleState.undoState.canUndo) titleState.undoState.undo()
            else if (descriptionState.undoState.canUndo) descriptionState.undoState.undo()
            else throw Exception("there is no field is focused!!")
        },
        enabled = titleState?.undoState?.canUndo!! or descriptionState?.undoState?.canUndo!!
    ) {
        Icon(
            painter = painterResource(id = UNDO),
            contentDescription = null,
            tint = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surfaceVariant),
        )
    }
    IconButton(
        onClick = {
            sound.performSoundEffect(context, KEY_CLICK, isSound)
            if (titleState.undoState.canRedo) titleState.undoState.redo()
            else if (descriptionState.undoState.canRedo) descriptionState.undoState.redo()
            else throw Exception("there is no field is focused!!")
        },
        enabled = titleState.undoState.canRedo or descriptionState.undoState.canRedo

    ) {
        Icon(
            painter = painterResource(id = REDO),
            contentDescription = null,
            tint = contentColorFor(backgroundColor = MaterialTheme.colorScheme.surfaceVariant),
        )
    }
}