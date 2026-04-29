package zouitel.neighborhood.assistant.ui.assistant

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.assistant.ai.LocalToolOption
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.model.AssistantRegex
import zouitel.neighborhood.assistant.model.QuickMessage
import zouitel.neighborhood.assistant.ui.ai_model.Models
import zouitel.neighborhood.assistant.ui.ai_model.ModelsScreenModel
import zouitel.neighborhood.assistant.ui.configurations.ModelConfigurationScreen
import zouitel.neighborhood.assistant.ui.provider.ProvidersScreen
import zouitel.neighborhood.assistant.utils.AdditionalLayout
import zouitel.neighborhood.assistant.utils.BodySheet
import zouitel.neighborhood.assistant.utils.ChooserItem
import zouitel.neighborhood.assistant.utils.HeaderSheet
import zouitel.neighborhood.assistant.utils.MessageRegexSheet
import zouitel.neighborhood.assistant.utils.QuickMessageSheet
import zouitel.neighborhood.assistant.utils.ResetSheet
import zouitel.neighborhood.assistant.utils.SaveSheet
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.ui.SettingsScreenModel
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import zouitel.neighborhood.assistant.R as AR
import zouitel.neighborhood.logic.R as LR
import zouitel.neighborhood.feedback.reportable
import kotlin.collections.minus
import kotlin.collections.plus
import kotlin.math.roundToInt
import kotlin.random.Random

class AssistantScreen(val assistant: Assistant?): Screen {
    @Composable
    override fun Content() {
        AssistantConfiguration(
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
        )
    }

    @SuppressLint("DefaultLocale")
    @Composable
    fun AssistantConfiguration(
        assistantModel: AssistantScreenModel,
        modelsModel: ModelsScreenModel,
        settingsModel: SettingsScreenModel
    ) {
        val navigator = LocalNavigator.current
        val bottomNav = LocalBottomSheetNavigator.current

        val settings by remember(settingsModel, settingsModel::observe).collectAsState()

        val scope = rememberCoroutineScope()
        val snackbarState = remember { SnackbarHostState() }

        val promptState = rememberTextFieldState(assistant?.systemPrompt ?: "")

        val jobCounter = remember { mutableIntStateOf(0) }
        val deletedModel = remember { mutableStateListOf<Pair<Model, Int>>() }
        val deletedMessage = remember { mutableStateListOf<Pair<QuickMessage, Int>>() }
        val deletedRegex = remember { mutableStateListOf<Pair<AssistantRegex, Int>>() }
        val deletedHeader = remember { mutableStateListOf<Pair<CustomHeader, Int>>() }
        val deletedBody = remember { mutableStateListOf<Pair<CustomBody, Int>>() }

        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

        var quickMessageSheet by remember { mutableStateOf(false) }
        var quickMessageState by remember { mutableStateOf(QuickMessage()) }
        var messageRegexSheet by remember { mutableStateOf(false) }
        var messageRegexState by remember {
            mutableStateOf(AssistantRegex(Random.nextInt().toShort()))
        }
        var headerSheet by remember { mutableStateOf(false) }
        var headerState by remember { mutableStateOf(CustomHeader("", "")) }
        var bodySheet by remember { mutableStateOf(false) }
        var bodyState by remember { mutableStateOf(CustomBody("", JsonPrimitive(""))) }
        var resetSheet by remember { mutableStateOf(false) }
        var saveSheet by remember { mutableStateOf(false) }

        val deletedResource = stringResource(LR.string.deleted)
        val undoResource = stringResource(LR.string.action_undo)

        val currentAssistant = remember(assistant) {
            mutableStateOf(
                assistant?.copy(systemPrompt = promptState.text.toString()) ?: Assistant()
            )
        }

        // Track if there are unsaved changes
        val hasUnsavedChanges by remember {
            derivedStateOf {
                val original = assistant ?: Assistant()
                val current =
                    currentAssistant.value.copy(systemPrompt = promptState.text.toString())

                original != current
            }
        }

        val cacheWindow = remember {
            LazyLayoutCacheWindow(500.dp, 500.dp)
        }
        val listState = rememberLazyListState(cacheWindow)

        // Handle back button press
        BackHandler(enabled = hasUnsavedChanges) {
            saveSheet = true
        }

        Scaffold(
            topBar = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    TopAppBar(
                        title = { Text(stringResource(LR.string.assistant_settings)) },
                        navigationIcon = {
                            IconButton({
                                if (hasUnsavedChanges) {
                                    saveSheet = true
                                } else {
                                    navigator?.pop()
                                }
                            }) {
                                Icon(painterResource(CommonIcons.ARROW_LEFT), null)
                            }
                        },
                        scrollBehavior = scrollBehavior
                    )
                }
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
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(enabled = quickMessageSheet || messageRegexSheet || headerSheet || bodySheet || resetSheet || saveSheet || bottomNav.isVisible)
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                item {
                    AdditionalLayout(
                        stringResource(LR.string.ai_models),
                        null,
                        emptyArray<Unit>(),
                        {
                            navigator?.push(ProvidersScreen())
                        },
                        {}
                    ) {}
                    Models(
                        assistantModel,
                        modelsModel,
                        true,
                        {
                            navigator?.push(ModelConfigurationScreen(it))
                        }
                    ) {
                        val currentJobId = jobCounter.intValue++
                        deletedModel.add(Pair(it, currentJobId))
                        val modelPair =
                            deletedModel.firstOrNull { pair -> pair.second == currentJobId }

                        modelsModel.delete(it.modelId)

                        scope.launch {
                            snackbarState.currentSnackbarData?.dismiss()
                            val result = snackbarState.showSnackbar(
                                message = "${modelPair?.first?.displayName} $deletedResource",
                                actionLabel = undoResource,
                                duration = SnackbarDuration.Long
                            )

                            if (result == SnackbarResult.ActionPerformed) {
                                modelPair?.first?.let { model ->
                                    modelsModel.upsert(model)
                                }
                            }
                            modelPair?.let { model -> deletedModel.remove(model) }
                        }
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.learning_mode),
                        stringResource(LR.string.learning_mode_description).trimIndent(),
                        {
                            AsyncImage(
                                AR.drawable.creative_brain,
                                null,
                                contentScale = ContentScale.Crop
                            )
                        },
                        currentAssistant.value.learningMode,
                        {
                            currentAssistant.value = currentAssistant.value.copy(learningMode = it)
                        },
                        {}
                    ) {}
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.temperature),
                        stringResource(LR.string.temperature_description).trimIndent(),
                        null,
                        checked = currentAssistant.value.temperature != null,
                        onChecked = {
                            if (it) {
                                currentAssistant.value =
                                    currentAssistant.value.copy(temperature = 1f)
                            } else {
                                currentAssistant.value =
                                    currentAssistant.value.copy(temperature = null)
                            }
                        },
                        content = {
                            val tooltipState = rememberTooltipState(isPersistent = true)
                            val scope = rememberCoroutineScope()

                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = {
                                    PlainTooltip {
                                        Text(
                                            String.format(
                                                "%.2f",
                                                currentAssistant.value.temperature ?: 1f
                                            ),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                },
                                state = tooltipState,
                                focusable = true
                            ) {
                                Slider(
                                    value = currentAssistant.value.temperature ?: 1f,
                                    onValueChange = {
                                        currentAssistant.value =
                                            currentAssistant.value.copy(temperature = it)
                                        scope.launch {
                                            tooltipState.show()
                                        }
                                    },
                                    valueRange = 0f..2f,
                                    enabled = currentAssistant.value.temperature != null,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = CommonColors.prime
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.top_p),
                        stringResource(LR.string.top_p_description).trimIndent(),
                        null,
                        checked = currentAssistant.value.topP != null,
                        onChecked = {
                            if (it) {
                                currentAssistant.value = currentAssistant.value.copy(topP = 1f)
                            } else {
                                currentAssistant.value = currentAssistant.value.copy(topP = null)
                            }
                        },
                        content = {
                            val tooltipState = rememberTooltipState(isPersistent = true)
                            val scope = rememberCoroutineScope()

                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = {
                                    PlainTooltip {
                                        Text(
                                            String.format(
                                                "%.2f",
                                                currentAssistant.value.topP ?: 1f
                                            ),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                },
                                state = tooltipState
                            ) {
                                Slider(
                                    value = currentAssistant.value.topP ?: 1f,
                                    onValueChange = {
                                        currentAssistant.value =
                                            currentAssistant.value.copy(topP = it)
                                        scope.launch {
                                            tooltipState.show()
                                        }
                                    },
                                    enabled = currentAssistant.value.topP != null,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = CommonColors.prime
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.context_message_size),
                        stringResource(LR.string.context_message_size_description).trimIndent(),
                        null,
                        null,
                        content = {
                            val tooltipState = rememberTooltipState(isPersistent = true)
                            val scope = rememberCoroutineScope()

                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                tooltip = {
                                    PlainTooltip {
                                        Text(
                                            "${currentAssistant.value.contextMessageSize}",
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                },
                                state = tooltipState
                            ) {
                                Slider(
                                    value = currentAssistant.value.contextMessageSize.toFloat(),
                                    onValueChange = {
                                        currentAssistant.value =
                                            currentAssistant.value.copy(contextMessageSize = it.roundToInt())
                                        scope.launch {
                                            tooltipState.show()
                                        }
                                    },
                                    valueRange = 1f..512f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = CommonColors.prime
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp)
                                )
                            }
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.stream_output),
                        stringResource(LR.string.stream_output_description),
                        null,
                        currentAssistant.value.streamOutput,
                        {
                            currentAssistant.value = currentAssistant.value.copy(streamOutput = it)
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    ChooserItem(
                        currentAssistant,
                        stringResource(LR.string.thinking_budget),
                        stringResource(LR.string.thinking_budget_description).trimIndent(),
                        currentAssistant.value.thinkingBudget != null
                    ) {
                        currentAssistant.value = if (it) {
                            currentAssistant.value.copy(thinkingBudget = -1)
                        } else {
                            currentAssistant.value.copy(thinkingBudget = null)
                        }
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    val systemPrompts =
                        linkedMapOf(
                            stringResource(LR.string.date) to "{{cur_date}}",
                            stringResource(LR.string.time) to "{{cur_time}}",
                            stringResource(LR.string.date_and_time) to "{{cur_datetime}}",
                            stringResource(LR.string.model_name) to "{{model_name}}",
                            stringResource(LR.string.local) to "{{local}}",
                            stringResource(LR.string.timezone) to "{{timezone}}",
                            stringResource(LR.string.system_version) to "{{system_version}}",
                            stringResource(LR.string.device_info) to "{{device_info}}",
                            stringResource(LR.string.battery_level) to "{{battery_level}}"
                        )

                    val promptsEntries = remember(systemPrompts) { systemPrompts.entries.toList() }

                    CommonActivationItem(
                        stringResource(LR.string.system_prompt),
                        null,
                        null,
                        null,
                        content = {
                            CommonTextField(
                                state = promptState,
                                modifier = Modifier.heightIn(220.dp),
                                placeholder = stringResource(LR.string.prompt)
                            )
                            Text("${stringResource(LR.string.properties)}:")
                            ContextualFlowRow(systemPrompts.size) { index ->
                                val (budgetKey, itemValue) = promptsEntries[index]
                                ElevatedAssistChip(
                                    { promptState.edit { append(itemValue) } },
                                    { Text(budgetKey) },
                                    Modifier.padding(horizontal = 3.dp)
                                )
                            }
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        stringResource(LR.string.quick_messages),
                        stringResource(LR.string.quick_messages_description),
                        currentAssistant.value.quickMessages.toTypedArray(),
                        {
                            quickMessageSheet = true
                        },
                        {
                            val currentJobId = jobCounter.intValue++
                            deletedMessage.add(Pair(it, currentJobId))
                            val pair =
                                deletedMessage.firstOrNull { pair -> pair.second == currentJobId }

                            // delete the element.
                            currentAssistant.value = currentAssistant.value.copy(
                                quickMessages = currentAssistant.value.quickMessages.minusElement(it)
                            )

                            scope.launch {
                                snackbarState.currentSnackbarData?.dismiss()
                                val result = snackbarState.showSnackbar(
                                    message = "${pair?.first?.title} $deletedResource",
                                    actionLabel = undoResource,
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    pair?.first?.let { message ->
                                        // undelete the element.
                                        currentAssistant.value = currentAssistant.value.copy(
                                            quickMessages = currentAssistant.value.quickMessages.plusElement(
                                                message
                                            )
                                        )
                                    }
                                }
                                pair?.let { message -> deletedMessage.remove(message) }
                            }
                        }
                    ) { message ->
                        quickMessageState = message
                        quickMessageSheet = true
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        stringResource(LR.string.regular_expressions),
                        stringResource(LR.string.regular_expressions_description).trimIndent(),
                        currentAssistant.value.regexes.toTypedArray(),
                        {
                            messageRegexSheet = true
                        },
                        {
                            val currentJobId = jobCounter.intValue++
                            deletedRegex.add(Pair(it, currentJobId))
                            val pair =
                                deletedRegex.firstOrNull { pair -> pair.second == currentJobId }

                            // delete the element.
                            currentAssistant.value = currentAssistant.value.copy(
                                regexes = currentAssistant.value.regexes.minusElement(it)
                            )

                            scope.launch {
                                snackbarState.currentSnackbarData?.dismiss()
                                val result = snackbarState.showSnackbar(
                                    message = "${pair?.first?.name} $deletedResource",
                                    actionLabel = undoResource,
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    pair?.first?.let { regex ->
                                        // undelete the element.
                                        currentAssistant.value = currentAssistant.value.copy(
                                            regexes = currentAssistant.value.regexes.plusElement(
                                                regex
                                            )
                                        )
                                    }
                                }
                                pair?.let { regex -> deletedRegex.remove(regex) }
                            }
                        }
                    ) { regex ->
                        messageRegexState = regex
                        messageRegexSheet = true
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        stringResource(LR.string.headers),
                        stringResource(LR.string.headers_description).trimIndent(),
                        currentAssistant.value.customHeaders.toTypedArray(),
                        { headerSheet = true },
                        {
                            val currentJobId = jobCounter.intValue++
                            deletedHeader.add(Pair(it, currentJobId))
                            val pair =
                                deletedHeader.firstOrNull { pair -> pair.second == currentJobId }

                            // delete the element.
                            currentAssistant.value = currentAssistant.value.copy(
                                customHeaders = currentAssistant.value.customHeaders.minusElement(it)
                            )

                            scope.launch {
                                snackbarState.currentSnackbarData?.dismiss()
                                val result = snackbarState.showSnackbar(
                                    message = "${pair?.first?.name} $deletedResource",
                                    actionLabel = undoResource,
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    pair?.first?.let { header ->
                                        // undelete the element.
                                        currentAssistant.value = currentAssistant.value.copy(
                                            customHeaders = currentAssistant.value.customHeaders.plusElement(
                                                header
                                            )
                                        )
                                    }
                                }
                                pair?.let { header -> deletedHeader.remove(header) }
                            }
                        }
                    ) { header ->
                        headerState = header
                        headerSheet = true
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        stringResource(LR.string.bodies),
                        stringResource(LR.string.bodies_description).trimIndent(),
                        currentAssistant.value.customBodies.toTypedArray(),
                        { bodySheet = true },
                        {
                            val currentJobId = jobCounter.intValue++
                            deletedBody.add(Pair(it, currentJobId))
                            val pair =
                                deletedBody.firstOrNull { pair -> pair.second == currentJobId }

                            // delete the element.
                            currentAssistant.value = currentAssistant.value.copy(
                                customBodies = currentAssistant.value.customBodies.minusElement(it)
                            )

                            scope.launch {
                                snackbarState.currentSnackbarData?.dismiss()
                                val result = snackbarState.showSnackbar(
                                    message = "${pair?.first?.key} $deletedResource",
                                    actionLabel = undoResource,
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    pair?.first?.let { header ->
                                        // undelete the element.
                                        currentAssistant.value = currentAssistant.value.copy(
                                            customBodies = currentAssistant.value.customBodies.plusElement(
                                                header
                                            )
                                        )
                                    }
                                }
                                pair?.let { body -> deletedBody.remove(body) }
                            }
                        }
                    ) { body ->
                        bodyState = body
                        bodySheet = true
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.memory) ,
                        stringResource(LR.string.memory_description).trimIndent(),
                        null,
                        currentAssistant.value.enableMemory,
                        { currentAssistant.value = currentAssistant.value.copy(enableMemory = it) }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.chats_reference),
                        stringResource(LR.string.chats_reference_description).trimIndent(),
                        null,
                        currentAssistant.value.enableRecentChatsReference,
                        {
                            currentAssistant.value =
                                currentAssistant.value.copy(enableRecentChatsReference = it)
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        stringResource(LR.string.javascript),
                        stringResource(LR.string.javascript_description).trimIndent(),
                        {
                            Image(painterResource(AR.drawable.js), null)
                        },
                        currentAssistant.value.localTools.contains(LocalToolOption.JavascriptEngine),
                        {
                            currentAssistant.value = if (it) {
                                currentAssistant.value.copy(
                                    localTools = currentAssistant.value.localTools.plus(
                                        LocalToolOption.JavascriptEngine
                                    )
                                )
                            } else {
                                currentAssistant.value.copy(
                                    localTools = currentAssistant.value.localTools.minus(
                                        LocalToolOption.JavascriptEngine
                                    )
                                )
                            }
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(vertical = 30.dp),
                        Arrangement.Center
                    ) {
                        FilledTonalButton(
                            { resetSheet = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(
                                stringResource(LR.string.reset),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            if (quickMessageSheet) {
                QuickMessageSheet(
                    quickMessageState,
                    { quickMessageSheet = false }
                ) {
                    if (it == quickMessageState) {
                        currentAssistant.value = currentAssistant.value.copy(
                            quickMessages = currentAssistant.value.quickMessages.minusElement(it)
                        )
                    }
                    currentAssistant.value = currentAssistant.value.copy(
                        quickMessages = currentAssistant.value.quickMessages.plusElement(it)
                    )
                }
            }
            if (messageRegexSheet) {
                MessageRegexSheet(
                    messageRegexState,
                    { messageRegexSheet = false }
                ) {
                    if (it == messageRegexState) {
                        currentAssistant.value = currentAssistant.value.copy(
                            regexes = currentAssistant.value.regexes.minusElement(it)
                        )
                    }
                    currentAssistant.value = currentAssistant.value.copy(
                        regexes = currentAssistant.value.regexes.plusElement(it)
                    )
                }
            }
            if (headerSheet) {
                HeaderSheet(
                    headerState,
                    { headerSheet = false }
                ) {
                    if (it == headerState) {
                        currentAssistant.value = currentAssistant.value.copy(
                            customHeaders = currentAssistant.value.customHeaders.minusElement(it)
                        )
                    }
                    currentAssistant.value = currentAssistant.value.copy(
                        customHeaders = currentAssistant.value.customHeaders.plusElement(it)
                    )
                }
            }
            if (bodySheet) {
                BodySheet(
                    bodyState,
                    { bodySheet = false }
                ) {
                    if (it == bodyState) {
                        currentAssistant.value = currentAssistant.value.copy(
                            customBodies = currentAssistant.value.customBodies.minusElement(it)
                        )
                    }
                    currentAssistant.value = currentAssistant.value.copy(
                        customBodies = currentAssistant.value.customBodies.plusElement(it)
                    )
                }
            }
            if (resetSheet) {
                ResetSheet({ resetSheet = false }) {
                    currentAssistant.value = Assistant()
                    promptState.clearText()
                }
            }
            if (saveSheet) {
                SaveSheet(
                    {
                        assistantModel.upsert(currentAssistant.value.copy(systemPrompt = promptState.text.toString()))
                        navigator?.pop()
                    },
                    { navigator?.pop() }
                ) { saveSheet = false }
            }
        }
    }
}