package zouitel.neighborhood.assistant.ui.configurations

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
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
import zouitel.neighborhood.feedback.reportable
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

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

        var modelDetails by remember { mutableStateOf<Details?>(null) }
        LaunchedEffect(model.modelId) {
            providerModel.initProviderId(model.providerId)
            modelDetails = Details(model)
        }

        val providerOfModel by remember(providerModel, providerModel::observeById).collectAsState()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()

        var currentModel by remember { mutableStateOf(model) }
        val modalSheetState = rememberModalBottomSheetState()
        val scaffoldState = rememberBottomSheetScaffoldState(modalSheetState)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val scope = rememberCoroutineScope()
        val snackbarState = remember { SnackbarHostState() }

        var currentSheet: SheetContent by remember { mutableStateOf(SheetContent.None) }
        val closeSheet: () -> Unit = {
            scope.launch { scaffoldState.bottomSheetState.hide() }
            currentSheet = SheetContent.None // Clear content after collapse animation finishes
        }
        val openSheet: (SheetContent) -> Unit = { sheet ->
            currentSheet = sheet
            scope.launch { scaffoldState.bottomSheetState.show() }
        }
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
            openSheet(
                SheetContent.Save(
                onSave = {
                    modelsModel.upsert(currentModel)
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
                    title = { Text(model.displayName) },
                    navigationIcon = {
                        IconButton({
                            if (hasUnsavedChanges) {
                                openSheet(
                                    SheetContent.Save(
                                    onSave = {
                                        modelsModel.upsert(currentModel)
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
                        actionColor = CommonColors.azure
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(enabled = scaffoldState.bottomSheetState.isVisible || bottomNav.isVisible)
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
                                    activeContainerColor = CommonColors.azure,
                                    activeContentColor = Color.White,
                                    activeBorderColor = CommonColors.azure
                                )
                            ) {
                                Text(type.name, style = MaterialTheme.typography.bodyMedium)
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
                                        activeContainerColor = CommonColors.azure,
                                        activeContentColor = Color.White,
                                        activeBorderColor = CommonColors.azure
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
                                        activeContainerColor = CommonColors.azure,
                                        activeContentColor = Color.White,
                                        activeBorderColor = CommonColors.azure
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
                                        activeContainerColor = CommonColors.azure,
                                        activeContentColor = Color.White,
                                        activeBorderColor = CommonColors.azure
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
                        "",
                        currentModel.customHeaders.toTypedArray(),
                        {
                            openSheet(SheetContent.Header(CustomHeader("", "")) {
                                currentModel = currentModel.copy(
                                    customHeaders = currentModel.customHeaders.plusElement(
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
                        openSheet(SheetContent.Header(header) {
                            currentModel = currentModel.copy(
                                customHeaders = currentModel.customHeaders.minusElement(
                                    header
                                ).plusElement(it)
                            )
                        })
                    }
                }
                item {
                    AdditionalLayout(
                        "Bodies",
                        "",
                        currentModel.customBodies.toTypedArray(),
                        {
                            openSheet(SheetContent.Body(CustomBody("", JsonPrimitive(""))) {
                                currentModel = currentModel.copy(
                                    customBodies = currentModel.customBodies.plusElement(
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
                        openSheet(SheetContent.Body(body) {
                            currentModel = currentModel.copy(
                                customBodies = currentModel.customBodies.minusElement(
                                    body
                                ).plusElement(it)
                            )
                        })
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
                        null,
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
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        Arrangement.Center
                    ) {
                        FilledTonalButton(
                            {
                                openSheet(SheetContent.Reset {
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
                                })
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
        }
    }
}