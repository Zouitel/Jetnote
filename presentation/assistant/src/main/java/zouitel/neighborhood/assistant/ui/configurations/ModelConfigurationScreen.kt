package zouitel.neighborhood.assistant.ui.configurations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import coil.compose.AsyncImage
import zouitel.neighborhood.ai.provider.BuiltInTools
import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.provider.Modality
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.provider.ModelAbility
import zouitel.neighborhood.ai.provider.ModelType
import zouitel.neighborhood.assistant.model.SheetContent
import zouitel.neighborhood.assistant.ui.ai_model.ModelsScreenModel
import zouitel.neighborhood.assistant.ui.provider.ProviderScreenModel
import zouitel.neighborhood.assistant.utils.AdditionalLayout
import zouitel.neighborhood.assistant.utils.SheetLayout
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.utils.BodySheet
import zouitel.neighborhood.assistant.utils.HeaderSheet
import zouitel.neighborhood.assistant.utils.ResetSheet
import zouitel.neighborhood.assistant.utils.SaveSheet
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

class ModelConfigurationScreen(val model: Model): Screen {
    @Composable
    override fun Content() {
        ModelConfiguration(
            model,
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun ModelConfiguration(
        model: Model,
        modelsModel: ModelsScreenModel,
        providerModel: ProviderScreenModel,
        settingsModel: SettingsScreenModel

    ) {
        val navigator = LocalNavigator.current
        val bottomNav = LocalBottomSheetNavigator.current

//        var modelDetails by remember { mutableStateOf<Details?>(null) }
        LaunchedEffect(model.modelId) {
            providerModel.initProviderId(model.providerId)
//            modelDetails = Details(model)
        }

        val providerOfModel by remember(providerModel, providerModel::observeById).collectAsState()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()

        var currentModel by remember { mutableStateOf(model) }
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val scope = rememberCoroutineScope()
        val snackbarState = remember { SnackbarHostState() }
        val (lazyListState, listHeight) = rememberAutoHeightCacheLazyList()

        var headerSheet by remember { mutableStateOf(false) }
        var headerState by remember { mutableStateOf(CustomHeader("", "")) }
        var bodySheet by remember { mutableStateOf(false) }
        var bodyState by remember { mutableStateOf(CustomBody("", JsonPrimitive(""))) }
        var resetSheet by remember { mutableStateOf(false) }
        var saveSheet by remember { mutableStateOf(false) }

        val jobCounter = remember { mutableIntStateOf(0) }
        val deletedHeader = remember { mutableStateListOf<Pair<CustomHeader, Int>>() }
        val deletedBody = remember { mutableStateListOf<Pair<CustomBody, Int>>() }

        // Track if there are unsaved changes
        val hasUnsavedChanges by remember {
            derivedStateOf {
                val original = model
                val current = currentModel

                original != current
            }
        }

        // Handle back button press
        BackHandler(enabled = hasUnsavedChanges) {
            saveSheet = true
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(model.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton({
                            if (hasUnsavedChanges) {
                                saveSheet = true
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
                .cloudy(enabled = headerSheet || bodySheet || resetSheet || saveSheet || bottomNav.isVisible)
        ) { contentPadding ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    CommonActivationItem(
                        "Details",
                        null, null, null,
                        content = {
                            Details(model).Properties()
                        }
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    Text(
                        "Model type",
                        Modifier.padding(horizontal = 8.dp),
                        fontSize = 19.sp
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.padding(horizontal = 8.dp), (-9).dp) {
                        ModelType.entries.forEach { type ->
                            SegmentedButton(
                                currentModel.type == type,
                                {
                                    currentModel = currentModel.copy(type = type)
                                },
                                MaterialTheme.shapes.extraLarge,
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = CommonColors.prime,
                                    activeContentColor = MaterialTheme.colorScheme.surface,
                                    activeBorderColor = CommonColors.prime
                                )
                            ) {
                                Text(
                                    type.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,                    // ← Prevent wrapping
                                    softWrap = false,                // ← Force single line
                                    overflow = TextOverflow.Ellipsis // ← Add ellipsis if too long
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    Column(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .cloudy(enabled = currentModel.type != ModelType.CHAT)
                    ) {
                        Text(
                            "Input Modalities",
                            fontSize = 19.sp
                        )
                        MultiChoiceSegmentedButtonRow(space = (-9).dp) {
                            Modality.entries.forEach { modality ->
                                SegmentedButton(
                                    modality in currentModel.inputModalities,
                                    {
                                        currentModel =
                                            if (currentModel.inputModalities.contains(modality)) {
                                                currentModel.copy(
                                                    inputModalities = currentModel.inputModalities.minusElement(
                                                        modality
                                                    )
                                                )
                                            } else {
                                                currentModel.copy(
                                                    inputModalities = currentModel.inputModalities.plusElement(
                                                        modality
                                                    )
                                                )
                                            }
                                    },
                                    MaterialTheme.shapes.extraLarge,
                                    enabled = currentModel.type == ModelType.CHAT,
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = CommonColors.prime,
                                        activeContentColor = MaterialTheme.colorScheme.surface,
                                        activeBorderColor = CommonColors.prime
                                    )
                                ) {
                                    Text(modality.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    Column(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .cloudy(enabled = currentModel.type != ModelType.CHAT)
                    ) {
                        Text(
                            "Output Modalities",
                            fontSize = 19.sp
                        )
                        MultiChoiceSegmentedButtonRow(space = (-9).dp) {
                            Modality.entries.forEach { modality ->
                                SegmentedButton(
                                    modality in currentModel.outputModalities,
                                    {
                                        currentModel =
                                            if (currentModel.outputModalities.contains(modality)) {
                                                currentModel.copy(
                                                    outputModalities = currentModel.outputModalities.minusElement(
                                                        modality
                                                    )
                                                )
                                            } else {
                                                currentModel.copy(
                                                    outputModalities = currentModel.outputModalities.plusElement(
                                                        modality
                                                    )
                                                )
                                            }
                                    },
                                    MaterialTheme.shapes.extraLarge,
                                    enabled = currentModel.type == ModelType.CHAT,
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = CommonColors.prime,
                                        activeContentColor = MaterialTheme.colorScheme.surface,
                                        activeBorderColor = CommonColors.prime
                                    )
                                ) {
                                    Text(
                                        modality.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    Column(
                        Modifier
                            .padding(horizontal = 8.dp)
                            .cloudy(enabled = currentModel.type != ModelType.CHAT)
                    ) {
                        Text(
                            "Abilities",
                            fontSize = 19.sp
                        )
                        MultiChoiceSegmentedButtonRow(space = (-9).dp) {
                            ModelAbility.entries.forEach { ability ->
                                SegmentedButton(
                                    ability in currentModel.abilities,
                                    {
                                        currentModel =
                                            if (currentModel.abilities.contains(ability)) {
                                                currentModel.copy(
                                                    abilities = currentModel.abilities.minusElement(
                                                        ability
                                                    )
                                                )
                                            } else {
                                                currentModel.copy(
                                                    abilities = currentModel.abilities.plusElement(
                                                        ability
                                                    )
                                                )
                                            }
                                    },
                                    MaterialTheme.shapes.extraLarge,
                                    enabled = currentModel.type == ModelType.CHAT,
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = CommonColors.prime,
                                        activeContentColor = MaterialTheme.colorScheme.surface,
                                        activeBorderColor = CommonColors.prime
                                    )
                                ) {
                                    Text(ability.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        "Add provider override",
                        """
                            Configure custom provider settings for this
                            specific model.
                            This do use common properties from the
                            main provider of the model.
                        """.trimIndent(),
                        null,
                        currentModel.providerOverwrite != null,
                        {
                            currentModel = currentModel.copy(
                                providerOverwrite = if (it) providerOfModel else null
                            )
                        }
                    )
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
                        currentModel.customHeaders.toTypedArray(),
                        { headerSheet = true },
                        {
                            val currentJobId = jobCounter.intValue++
                            deletedHeader.add(Pair(it, currentJobId))
                            val pair =
                                deletedHeader.firstOrNull { pair -> pair.second == currentJobId }

                            // delete the element.
                            currentModel = currentModel.copy(
                                customHeaders = currentModel.customHeaders.minusElement(it)
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
                                        currentModel = currentModel.copy(
                                            customHeaders = currentModel.customHeaders.plusElement(
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
                        currentModel.customBodies.toTypedArray(),
                        { bodySheet = true },
                        {
                            val currentJobId = jobCounter.intValue++
                            deletedBody.add(Pair(it, currentJobId))
                            val pair =
                                deletedBody.firstOrNull { pair -> pair.second == currentJobId }

                            // delete the element.
                            currentModel = currentModel.copy(
                                customBodies = currentModel.customBodies.minusElement(it)
                            )

                            scope.launch {
                                snackbarState.currentSnackbarData?.dismiss()
                                val result = snackbarState.showSnackbar(
                                    message = "${pair?.first?.key} deleted",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    pair?.first?.let { body ->
                                        // undelete the element.
                                        currentModel = currentModel.copy(
                                            customBodies = currentModel.customBodies.plusElement(
                                                body
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
                        "Build Tools",
                        """
                            Those tools are directly provided by the
                            API without requiring app support.
                            This feature is supported by Gemini API
                            only right now.
                        """.trimIndent(),
                        null,
                        null
                    )
                }
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    CommonActivationItem(
                        "Search",
                        "Enable Google Search integration",
                        {
                            AsyncImage(
                                "https://raw.githubusercontent.com/lobehub/lobe-icons/refs/heads/master/packages/static-png/light/google-color.png",
                                "google logo",
                                Modifier.size(27.dp)
                            )
                        },
                        BuiltInTools.Search in currentModel.tools,
                        {
                            currentModel = if (it) {
                                currentModel.copy(
                                    tools = currentModel.tools.plusElement(
                                        BuiltInTools.Search
                                    )
                                )
                            } else {
                                currentModel.copy(
                                    tools = currentModel.tools.minusElement(
                                        BuiltInTools.Search
                                    )
                                )
                            }
                        }
                    )
                }
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    CommonActivationItem(
                        "URL Context",
                        "Enable URL context processing",
                        null,
                        BuiltInTools.UrlContext in currentModel.tools,
                        {
                            currentModel = if (it) {
                                currentModel.copy(
                                    tools = currentModel.tools.plusElement(
                                        BuiltInTools.UrlContext
                                    )
                                )
                            } else {
                                currentModel.copy(
                                    tools = currentModel.tools.minusElement(
                                        BuiltInTools.UrlContext
                                    )
                                )
                            }
                        }
                    )
                }
                item { Spacer(Modifier.padding(vertical = 20.dp)) }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp, 20.dp),
                        Arrangement.Center
                    ) {
                        FilledTonalButton(
                            {
                                resetSheet = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(
                                "Reset ${currentModel.displayName}!",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
                if (headerSheet) {
                    HeaderSheet(
                        headerState,
                        { headerSheet = false }
                    ) {
                        currentModel = currentModel.copy(
                            customHeaders = currentModel.customHeaders.plusElement(it)
                        )
                    }
                }
                if (bodySheet) {
                    BodySheet(
                        bodyState,
                        { bodySheet = false }
                    ) {
                        currentModel = currentModel.copy(
                            customBodies = currentModel.customBodies.plusElement(it)
                        )
                    }
                }
                if (resetSheet) {
                    ResetSheet({ resetSheet = false }) {
                        currentModel = model.copy(
                            type = ModelType.CHAT,
                            inputModalities = listOf(Modality.TEXT),
                            outputModalities = listOf(Modality.TEXT),
                            abilities = emptyList(),
                            providerOverwrite = null,
                            customHeaders = emptyList(),
                            customBodies = emptyList(),
                            tools = emptySet()
                        )
                    }
                }
                if (saveSheet) {
                    SaveSheet(
                        {
                            modelsModel.upsert(currentModel)
                            navigator?.pop()
                        },
                        { navigator?.pop() }
                    ) { saveSheet = false }
                }
        }
    }
}