package zouitel.neighborhood.workspace.providers

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.MutableState
import cafe.adriel.voyager.core.registry.ScreenProvider
import zouitel.neighborhood.workspace.model.Data

data class Options(
    val uid: String,
    val data: MutableState<Data>,
    val titleState: TextFieldState?,
    val descriptionState: TextFieldState?,
    val priorityState: MutableState<Int>,
    val background: MutableState<Int>
): ScreenProvider
