package zouitel.neighborhood.assistant.ui.configurations

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
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
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
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.assistant.ai.LocalToolOption
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.model.AssistantRegex
import zouitel.neighborhood.assistant.model.QuickMessage
import zouitel.neighborhood.assistant.model.SheetContent
import zouitel.neighborhood.assistant.ui.ai_model.Models
import zouitel.neighborhood.assistant.ui.ai_model.ModelsScreenModel
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.assistant.ui.provider.ProvidersScreen
import zouitel.neighborhood.assistant.utils.AdditionalLayout
import zouitel.neighborhood.assistant.utils.ChooserItem
import zouitel.neighborhood.assistant.utils.SheetLayout
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.ui.SettingsScreenModel
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import zouitel.neighborhood.feedback.reportable
import kotlin.math.roundToInt
import kotlin.random.Random

class AssistantConfigurationScreen(val assistant: Assistant?): Screen {
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

        val modalSheetState = rememberModalBottomSheetState()
        val scaffoldState = rememberBottomSheetScaffoldState(modalSheetState)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

        var currentSheet: SheetContent by remember { mutableStateOf(SheetContent.None) }
        val closeSheet: () -> Unit = {
            scope.launch { scaffoldState.bottomSheetState.hide() }
            currentSheet = SheetContent.None // Clear content after collapse animation finishes
        }
        val openSheet: (SheetContent) -> Unit = { sheet ->
            currentSheet = sheet
            scope.launch { scaffoldState.bottomSheetState.show() }
        }

        // You should also handle setting currentSheet to None when the sheet is collapsed by a swipe
        if (scaffoldState.bottomSheetState.isVisible.not()) {
            currentSheet = SheetContent.None
        }
        val currentAssistant = remember(assistant) {
            mutableStateOf(
                assistant?.copy(systemPrompt = promptState.text.toString()) ?: Assistant()
            )
        }

        // Track if there are unsaved changes
        val hasUnsavedChanges by remember {
            derivedStateOf {
                val original = assistant ?: Assistant()
                val current = currentAssistant.value.copy(systemPrompt = promptState.text.toString())

                original != current
            }
        }

        // Handle back button press
        BackHandler(enabled = hasUnsavedChanges) {
            openSheet(SheetContent.Save(
                onSave = {
                    assistantModel.upsert(currentAssistant.value.copy(systemPrompt = promptState.text.toString()))
                    navigator?.pop()
                },
                onDiscard = {
                    navigator?.pop()
                }
            ))
        }

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = { Text("Assistant Configurations") },
                    navigationIcon = {
                        IconButton({
                            if (hasUnsavedChanges) {
                                openSheet(SheetContent.Save(
                                    onSave = {
                                        assistantModel.upsert(currentAssistant.value.copy(systemPrompt = promptState.text.toString()))
                                        navigator?.pop()
                                    },
                                    onDiscard = {
                                        navigator?.pop()
                                    }
                                ))
                            } else {
                                navigator?.pop()
                            }
                        }) {
                            Icon(painterResource(CommonIcons.ARROW_LEFT), "go back")
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            sheetContent = {
                SheetLayout(currentSheet, closeSheet)
            },
            snackbarHost = {
                SnackbarHost(snackbarState) { data ->
                    Snackbar(
                        snackbarData = data,
                        actionOnNewLine = false,
                        shape = MaterialTheme.shapes.extraLarge,
                        actionColor = CommonColors.prime
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(enabled = scaffoldState.bottomSheetState.isVisible || bottomNav.isVisible)
        ) { paddingValues ->
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                item {
                    AdditionalLayout(
                        "A.I Models",
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
                                message = "${modelPair?.first?.displayName} deleted",
                                actionLabel = "Undo",
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
                        "Learning Mode",
                        """
                    To predict an output for new, unseen data
                    based on the patterns learned from the
                    labeled examples.
                """.trimIndent(),
                        {
                            Image(
                                painterResource(R.drawable.creative_brain),
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
                        "Temperature",
                        """
           Controls the randomness of the output.
           Higher values are more creative.
           """.trimIndent(),
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
                                            String.format("%.2f", currentAssistant.value.temperature ?: 1f),
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
                        "Top P",
                        """
            Controls which tokens are considered for
            output.
            """.trimIndent(),
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
                                            String.format("%.2f", currentAssistant.value.topP ?: 1f),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                },
                                state = tooltipState
                            ) {
                                Slider(
                                    value = currentAssistant.value.topP ?: 1f,
                                    onValueChange = {
                                        currentAssistant.value = currentAssistant.value.copy(topP = it)
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
                        "Context message size",
                        """
            Maximum number of previous messages
            to include in the context.
        """.trimIndent(),
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
                        "Stream output",
                        "Display response as it's being generated.",
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
                        "Thinking budget",
                        """
                            Limits the computational resources used
                            for the response.
                        """.trimIndent(),
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
                    val promptsEntries = remember { systemPrompts.entries.toList() }

                    CommonActivationItem(
                        "System prompt",
                        null,
                        null,
                        null,
                        content = {
                            CommonTextField(
                                state = promptState,
                                modifier = Modifier.heightIn(220.dp),
                                placeholder = "Prompt.."
                            )
                            Text("Properties:")
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
                        "Quick messages",
                        "Configure quick message shortcuts.",
                        currentAssistant.value.quickMessages.toTypedArray(),
                        {
                            openSheet(SheetContent.Quick(QuickMessage()) { message ->
                                currentAssistant.value = currentAssistant.value.copy(
                                    quickMessages = currentAssistant.value.quickMessages.plusElement(
                                        message
                                    )
                                )
                            })
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
                                    message = "${pair?.first?.title} deleted",
                                    actionLabel = "Undo",
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
                        openSheet(SheetContent.Quick(message) {
                            currentAssistant.value = currentAssistant.value.copy(
                                quickMessages = currentAssistant.value.quickMessages.minusElement(
                                    message
                                ).plusElement(it)
                            )
                        })
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        "Regular expressions",
                        """
                            Define regex patterns to transform
                            messages before sending/receiving.
                        """.trimIndent(),
                        currentAssistant.value.regexes.toTypedArray(),
                        {
                            openSheet(
                                SheetContent.Regex(AssistantRegex(Random.nextInt().toShort())) {
                                    currentAssistant.value = currentAssistant.value.copy(
                                        regexes = currentAssistant.value.regexes.plusElement(it)
                                    )
                                })
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
                                    message = "${pair?.first?.name} deleted",
                                    actionLabel = "Undo",
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
                        openSheet(SheetContent.Regex(regex) {
                            currentAssistant.value = currentAssistant.value.copy(
                                regexes = currentAssistant.value.regexes
                                    .minusElement(regex)
                                    .plusElement(it)
                            )
                        })
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        "Headers",
                        """
                            These are key-value pairs (name: value)
                            that are added to the request's header
                            block.
                            They are typically used for providing
                            authentication (like API keys or tokens),
                            specifying the content type, or passing
                            metadata about the request or client.
                        """.trimIndent(),
                        currentAssistant.value.customHeaders.toTypedArray(),
                        {
                            openSheet(SheetContent.Header(CustomHeader("", "")) {
                                currentAssistant.value = currentAssistant.value.copy(
                                    customHeaders = currentAssistant.value.customHeaders.plusElement(
                                        it
                                    )
                                )
                            })
                        },
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
                                    message = "${pair?.first?.name} deleted",
                                    actionLabel = "Undo",
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
                        openSheet(SheetContent.Header(header) {
                            currentAssistant.value = currentAssistant.value.copy(
                                customHeaders = currentAssistant.value.customHeaders.minusElement(
                                    header
                                ).plusElement(it)
                            )
                        })
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    AdditionalLayout(
                        "Bodies",
                        """
                            This refers to the data payload (key: value
                            JSON structure) sent within the request
                            itself.
                            It is the primary means of sending
                            inputs to the AI model.
                            These customizations are typically used to add specific.
                        """.trimIndent(),
                        currentAssistant.value.customBodies.toTypedArray(),
                        {
                            openSheet(SheetContent.Body(CustomBody("", JsonPrimitive(""))) {
                                currentAssistant.value = currentAssistant.value.copy(
                                    customBodies = currentAssistant.value.customBodies.plusElement(
                                        it
                                    )
                                )
                            })
                        },
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
                                    message = "${pair?.first?.key} deleted",
                                    actionLabel = "Undo",
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
                        openSheet(SheetContent.Body(body) {
                            currentAssistant.value = currentAssistant.value.copy(
                                customBodies = currentAssistant.value.customBodies.minusElement(
                                    body
                                ).plusElement(it)
                            )
                        })
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        "Memory",
                        """
                            Allow the assistant to remember previous
                            turns in the conversation.
                        """.trimIndent(),
                        null,
                        currentAssistant.value.enableMemory,
                        { currentAssistant.value = currentAssistant.value.copy(enableMemory = it) }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        "Recent Chats Reference",
                        """
                        Reference data from your recent chats for
                        better contextual responses.    
                        """.trimIndent(),
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
                        "JavaScript engine",
                        """
                            Execute JavaScript code with QuickJS
                            engine for calculations and scripting.
                        """.trimIndent(),
                        {
                            Image(painterResource(R.drawable.js), null)
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
                        Modifier.fillMaxWidth(),
                        Arrangement.Center
                    ) {
                        FilledTonalButton({
                            openSheet(SheetContent.Reset {
                                currentAssistant.value = Assistant()
                                promptState.clearText()
                            })
                        },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(
                                "Reset configurations!",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }
        }
    }

    private val systemPrompts = hashMapOf(
        "Date" to "{{cur_date}}",
        "Time" to "{{cur_time}}",
        "Date and Time" to "{{cur_datetime}}",
        "Model Name" to "{{model_name}}",
        "Local" to "{{local}}",
        "Timezone" to "{{timezone}}",
        "System Version" to "{{system_version}}",
        "Device Info" to "{{device_info}}",
        "Battery Level" to "{{battery_level}}"
    )
}