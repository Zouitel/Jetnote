package zouitel.neighborhood.workspace.ui.workplace

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.link.ui.LinkScreen
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.logic.R
import zouitel.neighborhood.markdown.state.BlockTextStates
import zouitel.neighborhood.markdown.state.rememberEditorState
import zouitel.neighborhood.markdown.theme.CascadeEditorColors
import zouitel.neighborhood.markdown.theme.CascadeEditorTheme
import zouitel.neighborhood.markdown.ui.CascadeEditor
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.export.NoteExportDialog
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import zouitel.neighborhood.workspace.ui.bottom_bar.BottomBar
import zouitel.neighborhood.workspace.ui.features.AudioScreen
import zouitel.neighborhood.workspace.ui.features.MediaTop
import zouitel.neighborhood.workspace.ui.utils.remindersContext
import zouitel.neighborhood.workspace.ui.utils.tagsContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.uuid.Uuid

data class WorkplaceScreen(
    val data: Data,
    val isNew: Boolean = true
): Screen {

    @Composable
    override fun Content() {
        Workplace(
            dataModel = koinScreenModel(),
            tagModel = koinScreenModel(),
            noteAndTagModel = koinScreenModel(),
            linkModel = koinScreenModel(),
            alarmModel = koinScreenModel(),
            workplaceModel = koinScreenModel(),
            settingsModel = koinScreenModel()
        )
    }

    @OptIn(ExperimentalTime::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "NewApi", "RememberReturnType")
    @Composable
    private fun Workplace(
        dataModel: DataScreenModel,
        tagModel: TagScreenModel,
        noteAndTagModel: NoteAndTagScreenModel,
        linkModel: LinkScreenModel,
        alarmModel: AlarmManagerScreenModel,
        workplaceModel: WorkplaceScreenModel,
        settingsModel: SettingsScreenModel
    ) {
        val uid = Uuid.random().toString()
        val keyboardManager = LocalFocusManager.current
        val navBottomSheet = LocalBottomSheetNavigator.current
        val keyboard = LocalSoftwareKeyboardController.current

        val titleState = rememberTextFieldState(data.title)
        val editorState = rememberEditorState(data.blocks)
        val defaultBackground = MaterialTheme.colorScheme.surface.toArgb()
        val defaultTextColor = MaterialTheme.colorScheme.onSurface.toArgb()

        val backgroundState = remember { mutableStateOf(data.background ?: defaultBackground) }
        val textColorState  = remember { mutableStateOf(data.textColor  ?: defaultTextColor) }

        val currentData = remember(data) {
            mutableStateOf(
                data.copy(
                    title = titleState.text.toString(),
                    background = data.background ?: defaultBackground,
                    textColor = data.textColor ?: defaultTextColor
                )
            )
        }

        LaunchedEffect(isNew) {
            if (isNew) {
                currentData.value = currentData.value.copy(uid = uid)
                dataModel.sendAction(Action.Insert(currentData.value.copy(uid = uid)))
            }
        }

        val focus = remember { FocusRequester() }

        val observeNotesAndLabels by remember(
            noteAndTagModel,
            noteAndTagModel::observeAll
        ).collectAsState()

        val observeLabels by remember(tagModel, tagModel::observeAll).collectAsState()

        val filteredTags by remember {
            derivedStateOf {
                observeLabels.filter {
                    observeNotesAndLabels.contains(NoteAndTag(currentData.value.uid, it.id))
                }
            }
        }
        val snackbarState = remember { SnackbarHostState() }
        var showExportDialog by remember { mutableStateOf(false) }
        val toolTipState = rememberTooltipState(isPersistent = true)
        val scope = rememberCoroutineScope()
        val deletedReminders = remember { mutableStateListOf<Pair<Reminder, Int>>() }
        val deletedTagId = remember { mutableStateListOf<Pair<Long, Int>>() }
        val jobCounter = remember { mutableIntStateOf(0) }
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
//        val (lazyListState, height) = rememberAutoHeightCacheLazyList()

        val cascadeTheme = remember(backgroundState.value, textColorState.value) {
            val bg = Color(backgroundState.value)
            val noteText = Color(textColorState.value)
            val isDark = bg.luminance() < 0.5f
            if (isDark) {
                CascadeEditorTheme.dark().copy(
                    colors = CascadeEditorColors.dark().copy(text = noteText)
                )
            } else {
                CascadeEditorTheme.light().copy(
                    colors = CascadeEditorColors.light().copy(text = noteText)
                )
            }
        }

        // Replace your existing LaunchedEffect(screen.value) for focus with this:
        LaunchedEffect(Unit) {
            if (isNew) {
                delay(200.milliseconds)
                focus.requestFocus()
                keyboard?.show()
            }
        }

        Scaffold(
            containerColor = Color(backgroundState.value),
            modifier = Modifier
                .fillMaxSize()
                .cloudy(enabled = navBottomSheet.isVisible)
                .reportable(settings.isShakable, backgroundState.value),
            bottomBar = {
                BottomBar(
                    data = currentData,
                    titleState = titleState,
                    editorState = editorState,
                    isSound = settings.isSound,
                    dataModel = dataModel,
                    linkModel = linkModel,
                    backgroundState = backgroundState,
                    textColorState = textColorState
                )
            },
            snackbarHost = {
                SnackbarHost(snackbarState) { data ->
                    Snackbar(
                        snackbarData = data,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        ) {
            val textStates = remember { BlockTextStates() }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                    MediaTop(currentData)

                    TextButton(onClick = { showExportDialog = true }) {
                        Text("Export note")
                    }

                    CommonTextField(
                        state = titleState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(55.dp)
                            .focusRequester(focus),
                        placeholder = stringResource(R.string.title),
                        textSize = MaterialTheme.typography.headlineMedium.fontSize,
                        textColor = Color(textColorState.value),
                        imeAction = ImeAction.Next,
                        keyboardActions = {
                            keyboardManager.moveFocus(FocusDirection.Next)
                        }
                    ) {
                        workplaceModel.handleContentReceiver(currentData, it)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    remindersContext(
                        alarmModel,
                        snackbarState,
                        toolTipState,
                        currentData,
                        scope,
                        deletedReminders,
                        jobCounter
                    )

                    tagsContext(
                        currentData.value.uid,
                        filteredTags,
                        noteAndTagModel,
                        snackbarHostState = snackbarState,
                        scope = scope,
                        deletedTagId = deletedTagId,
                        jobCounter = jobCounter
                    )

                    item {
                        Navigator(AudioScreen(currentData, snackbarState))
                    }

                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Navigator(LinkScreen(currentData.value.uid, snackbarState))
                        }
                    }
                }

                // --- 3. CASCADE EDITOR SECTION (Takes remaining space) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    CascadeEditor(
                        stateHolder = editorState,
                        textStates = textStates,
                        modifier = Modifier.fillMaxSize(),
                        theme = cascadeTheme
                    )

                    // Smart Note Placeholder Logic
                    val showPlaceholder by remember {
                        derivedStateOf {
                            val blocks = editorState.state.blocks
                            blocks.isEmpty() || (blocks.size == 1 && textStates.getVisibleText(blocks.first().id).isNullOrEmpty())
                        }
                    }

                    if (showPlaceholder) {
                        val horizontalPadding = cascadeTheme.dimensions.blockHorizontalPadding
                        Text(
                            text = "Note",
                            color = Color(textColorState.value).copy(alpha = 0.4f),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        if (showExportDialog) {
            NoteExportDialog(
                note = currentData.value.copy(
                    title = titleState.text.toString(),
                    blocks = editorState.state.blocks
                ),
                onDismiss = { showExportDialog = false }
            )
        }
    }
}