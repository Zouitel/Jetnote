package zouitel.neighborhood.workspace.ui.workplace

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.domain.utils.Action
import zouitel.neighborhood.link.ui.LinkScreen
import zouitel.neighborhood.link.ui.LinkScreenModel
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.tag.model.NoteAndTag
import zouitel.neighborhood.tag.model.Tag
import zouitel.neighborhood.tag.ui.NoteAndTagScreenModel
import zouitel.neighborhood.tag.ui.TagScreenModel
import zouitel.neighborhood.workspace.model.Data
import zouitel.neighborhood.workspace.model.Reminder
import zouitel.neighborhood.workspace.model.Task
import zouitel.neighborhood.workspace.notifications.AlarmManagerScreenModel
import zouitel.neighborhood.workspace.ui.bottom_bar.BottomBar
import zouitel.neighborhood.workspace.ui.features.AddTaskScreen
import zouitel.neighborhood.workspace.ui.features.AudioScreen
import zouitel.neighborhood.workspace.ui.features.MediaTop
import zouitel.neighborhood.workspace.ui.utils.TaskItem
import zouitel.neighborhood.workspace.ui.utils.remindersContext
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList
import kotlin.uuid.Uuid

data class WorkplaceScreen(
    val data: Data,
    val isNew: Boolean = true
): Screen {

    @Composable
    override fun Content() {
        Workplace(
            dataModel = getScreenModel(),
            tagModel = getScreenModel(),
            noteAndTagModel = getScreenModel(),
            linkModel = getScreenModel(),
            alarmModel = getScreenModel(),
            workplaceModel = getScreenModel(),
            settingsModel = getScreenModel()
        )
    }

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
        val titleState = rememberTextFieldState(data.title)
        val descriptionState = rememberTextFieldState(data.description)
        val defaultBackground = MaterialTheme.colorScheme.surface.toArgb()
        val defaultTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
        val currentData = remember(data) {
            mutableStateOf(
                data.copy(
                    title = titleState.text.toString(),
                    description = descriptionState.text.toString(),
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
        val toolTipState = rememberTooltipState(isPersistent = true)
        val scope = rememberCoroutineScope()
        val deletedReminders = remember { mutableStateListOf<Pair<Reminder, Int>>() }
        val deletedTagId = remember { mutableStateListOf<Pair<Long, Int>>() }
        val deletedTask = remember { mutableStateListOf<Pair<Task, Int>>() }
        val jobCounter = remember { mutableIntStateOf(0) }
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val (lazyListState, height) = rememberAutoHeightCacheLazyList()

        LaunchedEffect(true) {
            if (isNew) focus.requestFocus()
        }

        Scaffold(
            containerColor = Color(currentData.value.background ?: defaultBackground),
            modifier = Modifier
                .fillMaxSize()
                .cloudy(enabled = navBottomSheet.isVisible)
                .reportable(settings.isShakable, currentData.value.background),
            bottomBar = {
                BottomBar(
                    data = currentData,
                    titleState = titleState,
                    descriptionState = descriptionState,
                    isSound = settings.isSound,
                    dataModel = dataModel,
                    linkModel = linkModel
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
            LazyColumn(
                Modifier
                    .animateContentSize()
                    .statusBarsPadding()
                    .then(height),
                lazyListState
            ) {
                // medias display.
                item {
                    MediaTop(currentData)
                }

                // The Title.
                item {
                    CommonTextField(
                        state = titleState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(55.dp)
                            .focusRequester(focus),
                        placeholder = "Title",
                        textSize = MaterialTheme.typography.headlineMedium.fontSize,
                        textColor = Color(currentData.value.textColor ?: defaultTextColor),
                        imeAction = ImeAction.Next,
                        keyboardActions = {
                            keyboardManager.moveFocus(FocusDirection.Next)
                        }
                    ) {
                        workplaceModel.handleContentReceiver(currentData, it)
                    }
                }

                // The Description.
                item {
                    CommonTextField(
                        state = descriptionState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(50.dp),
                        textSize = MaterialTheme.typography.titleLarge.fontSize,
                        placeholder = "Note",
                        textColor = Color(currentData.value.textColor ?: defaultTextColor)
                    ) {
                        workplaceModel.handleContentReceiver(currentData, it)
                    }
                }

                // display reminder chips.
                remindersContext(
                    alarmModel,
                    snackbarState,
                    toolTipState,
                    currentData,
                    scope,
                    deletedReminders,
                    jobCounter
                )

                // display the tags.
                tagsContext(
                    currentData.value.uid,
                    filteredTags,
                    noteAndTagModel,
                    snackbarState,
                    scope,
                    deletedTagId,
                    jobCounter
                )

                // Audios display.
                item {
                    Navigator(AudioScreen(currentData, snackbarState))
                }

                // Link display.
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Navigator(LinkScreen(currentData.value.uid, snackbarState))
                }

                // display the tasks list.
                tasksList(snackbarState, currentData, scope, deletedTask, jobCounter)

                // void space.
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(170.dp))
                }
            }
        }
    }

    private fun LazyListScope.tasksList(
        snackbarState: SnackbarHostState,
        currentData: MutableState<Data>,
        scope: CoroutineScope,
        deletedTask: SnapshotStateList<Pair<Task, Int>>,
        jobCounter: MutableIntState
    ) {
        var orderedList = currentData.value.tasks.sortedBy { it.isDone }

        items(orderedList, key = { it.id }) { task ->
            Spacer(modifier = Modifier.height(3.dp))
            TaskItem(
                currentData = currentData,
                task = task,
                onEdit = {
                    it.show(AddTaskScreen(task, currentData))
                },
                onDelete = {
                    orderedList = orderedList.filterNot { it.id == task.id }

                    val currentJobId = jobCounter.intValue++
                    deletedTask.add(Pair(task, currentJobId))
                    val taskPair = deletedTask.firstOrNull { it.second == currentJobId }
                    currentData.value = currentData.value.copy(
                        tasks = currentData.value.tasks.minusElement(task)
                    )

                    scope.launch {
                        snackbarState.currentSnackbarData?.dismiss()
                        val result = snackbarState.showSnackbar(
                            message = "${taskPair?.first?.item} deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Long
                        )

                        if (result == SnackbarResult.ActionPerformed) {
                            taskPair?.first?.let { task ->
                                currentData.value = currentData.value.copy(
                                    tasks = currentData.value.tasks.plusElement(task)
                                )
                            }
                        }
                        taskPair?.let { deletedTask.remove(it) }
                    }
                }
            )
        }
    }

    fun LazyListScope.tagsContext(
        uid: String,
        filteredTags: List<Tag>,
        noteAndTagModel: NoteAndTagScreenModel,
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        deletedTagId: SnapshotStateList<Pair<Long, Int>>,
        jobCounter: MutableIntState
    ) {
        item {
            ContextualFlowRow(
                itemCount = filteredTags.size,
                modifier = Modifier
                    .animateContentSize()
                    .padding(horizontal = 10.dp)
            ) { index ->
                SuggestionChip(
                    modifier = Modifier
                        .alpha(.8f)
                        .padding(1.5.dp),
                    onClick = {},
                    label = {
                        Text(
                            modifier = Modifier
                                .combinedClickable(onLongClick = {
                                    val currentJobId = jobCounter.intValue++
                                    deletedTagId.add(
                                        Pair(
                                            filteredTags.getOrNull(index)?.id
                                                ?: return@combinedClickable, currentJobId
                                        )
                                    )
                                    val tagPair =
                                        deletedTagId.firstOrNull { it.second == currentJobId }

                                    noteAndTagModel.sendAction(
                                        Action.DeleteById(
                                            filteredTags.getOrNull(index)?.id
                                                ?: return@combinedClickable
                                        )
                                    )

                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        val result = snackbarHostState.showSnackbar(
                                            message = "${filteredTags.getOrNull(index)?.label} deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Long
                                        )


                                        if (result == SnackbarResult.ActionPerformed) {
                                            tagPair?.first?.let { tag ->
                                                noteAndTagModel.sendAction(
                                                    Action.Insert(NoteAndTag(uid, tag))
                                                )
                                            }
                                        }
                                        tagPair?.let { deletedTagId.remove(it) }
                                    }
                                }) {},
                            text = filteredTags.getOrNull(index)?.label ?: ""
                        )
                    },
                    border = SuggestionChipDefaults.suggestionChipBorder(false),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        Color(
                            filteredTags.getOrNull(index)?.color
                                ?: MaterialTheme.colorScheme.surfaceContainerHigh.toArgb()
                        ).copy(alpha = .5f),
                        Color(
                            filteredTags.getOrNull(index)?.color
                                ?: MaterialTheme.colorScheme.onSurface.toArgb()
                        )
                    )
                )
            }
        }
    }
}