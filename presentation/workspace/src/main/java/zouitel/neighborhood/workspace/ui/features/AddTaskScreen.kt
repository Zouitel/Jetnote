package zouitel.neighborhood.workspace.ui.features

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.ui.CommonBottomSheet
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Task
import kotlin.random.Random

class AddTaskScreen(val currentTask: Task?, val currentData: MutableState<Data>): Screen {
    @Composable
    override fun Content() {
        CommonBottomSheet(currentData.value.background) {
            Task()
        }
    }

    @SuppressLint("UnusedContentLambdaTargetStateParameter")
    @Composable
    internal fun Task() {
        val keyboardManager = LocalFocusManager.current
        val haptic = LocalHapticFeedback.current
        val bottomNav = LocalBottomSheetNavigator.current
        var currentTask by remember { mutableStateOf<Task?>(currentTask) }
        val fieldState = rememberTextFieldState(currentTask?.item ?: "")
        val focusRequester = remember { FocusRequester() }
        val taskResource = stringResource(zouitel.neighborhood.logic.R.string.task_placeholder)
        val taskCannotBeEmptyResource =
            stringResource(zouitel.neighborhood.logic.R.string.task_cannot_be_empty)
        var placeholder by remember { mutableStateOf(taskResource) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(currentData.value.uid) {
            focusRequester.requestFocus()
        }

        Surface(
            color = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) {
            CommonTextField(
                state = fieldState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusEvent { keyboardManager.moveFocus(FocusDirection.Enter) },
                placeholder = placeholder,
                imeAction = ImeAction.Done,
                keyboardActions = {
                    val inputTask = fieldState.text.toString().trim()

                    if (inputTask.isEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        placeholder = taskCannotBeEmptyResource
                        scope.launch {
                            delay(3000)
                            placeholder = taskResource
                        }
                    } else {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//                        currentTask?.let { task ->
//                            currentData.value = currentData.value.copy(
//                                tasks = currentData.value.tasks
//                                    .minusElement(task)
//                                    .plusElement(task.copy(item = inputTask))
//                            )
//                            bottomNav.hide()
//                        } ?: runCatching {
//                            currentData.value = currentData.value.copy(
//                                tasks = currentData.value.tasks.plusElement(
//                                    Task(Random.nextLong(), item = inputTask)
//                                )
//                            )
                    }
                    fieldState.clearText()
                    currentTask = null
                })
        }
    }
}