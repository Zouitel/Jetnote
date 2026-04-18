package zouitel.neighborhood.assistant.ui.conversation

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.bottomSheet.LocalBottomSheetNavigator
import zouitel.neighborhood.ai.core.MessageRole
import zouitel.neighborhood.ai.provider.Model
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.assistant.VibrationHelper
import zouitel.neighborhood.assistant.model.Conversation
import zouitel.neighborhood.assistant.ui.ai_model.ModelsScreenModel
import zouitel.neighborhood.assistant.ui.assistant.AssistantScreenModel
import zouitel.neighborhood.assistant.ui.configurations.ModelConfigurationScreen
import zouitel.neighborhood.assistant.ui.provider.ProviderScreenModel
import zouitel.neighborhood.assistant.utils.createChatFilesByContents
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.SettingsScreenModel
import zouitel.neighborhood.ui.markdown.MarkdownBlock
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.assistant.ui.ai_model.FullModels
import zouitel.neighborhood.assistant.ui.provider.ProvidersScreen
import zouitel.neighborhood.assistant.utils.JsonInstant
import zouitel.neighborhood.assistant.utils.Avatar
import zouitel.neighborhood.assistant.utils.ModelSwitcherPopup
import zouitel.neighborhood.feedback.reportable
import kotlin.uuid.Uuid

class ChatScreen(val isNew: Boolean, val id: Uuid = Uuid.random()): Screen {
    @Composable
    override fun Content() {
        Chat(
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel(),
            getScreenModel()
        )
    }

    // Replace the initialization section in your Chat composable with this:
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnrememberedMutableState",
        "CoroutineCreationDuringComposition"
    )
    @Composable
    private fun Chat(
        settingsModel: SettingsScreenModel,
        conversationModel: ChatScreenModel,
        assistantModel: AssistantScreenModel,
        modelsModel: ModelsScreenModel,
        providerModel: ProviderScreenModel
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        var isInitialized by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // Initial setup
        scope.launch {
            if (!isInitialized) {
                conversationModel.chatService.addConversationReference(id)
                if (isNew) {
                    val exists = conversationModel.conversationExists(id)
                    if (!exists) {
                        conversationModel.upsert(Conversation(id))
                        delay(200)
                    }
                }

                conversationModel.initConversationIdService(id)
                delay(100)

                conversationModel.initConversationId(id)
                isInitialized = true
            }
        }

        // Re-initialize when coming back from another screen
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        Log.d("ChatScreen", "Screen resumed")
                        // Re-add reference in case it was removed
                        conversationModel.chatService.addConversationReference(id)
                    }
                    Lifecycle.Event.ON_PAUSE -> {
                        Log.d("ChatScreen", "Screen paused (navigating away)")
                        // Don't remove reference, might come back
                    }
                    else -> {}
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                // Only remove if screen is being destroyed
                if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    Log.d("ChatScreen", "Screen destroyed - removing reference")
                    conversationModel.chatService.removeConversationReference(id)
                }
            }
        }

        // Rest of your Chat composable code continues here...
        val navigator = LocalNavigator.current
        val bottomNav = LocalBottomSheetNavigator.current
        val keyboardController = LocalSoftwareKeyboardController.current

        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val optionsSheet = remember { mutableStateOf(false) }
        val optionsState = rememberModalBottomSheetState()
        val modelsSheet = remember { mutableStateOf(false) }
        val modelsState = rememberModalBottomSheetState()

        val conversation by remember(
            conversationModel,
            conversationModel::observeById
        ).collectAsState()
        val settings by remember(settingsModel, settingsModel::observe).collectAsState()
        val assistant by remember(assistantModel, assistantModel::observe).collectAsState()
        val models by remember(modelsModel, modelsModel::observeAll).collectAsState(emptyList())
        val model by remember {
            derivedStateOf {
                models.find { it.uid == assistant?.currentModelUid }
            }
        }

        val provider by remember(providerModel, providerModel::observeById).collectAsState()

        val conversationJobs = remember(conversationModel, conversationModel::conversationJobs).collectAsState()
        val expanded = remember { mutableStateOf(false) }

        val inputState = rememberChatInputState()
        val listState = rememberLazyListState()
        val snackbarState = remember { SnackbarHostState() }
        val jobCounter = remember { mutableIntStateOf(0) }
        val deletedChat = remember { mutableStateListOf<Pair<Conversation, Int>>() }
        val showModelSwitcher = remember { mutableStateOf(false) }   // NEW

        // Load model logo
        LaunchedEffect(model) {
            providerModel.initProviderId(model?.providerId ?: return@LaunchedEffect)
            inputState.currentLogo = model?.logoUrl ?: return@LaunchedEffect
            inputState.currentModel = JsonInstant.encodeToString(model)
        }

        // Auto-scroll when new messages arrive
        LaunchedEffect(conversation?.nodes) {
            val itemCount = conversation?.nodes?.size ?: 0
            if (itemCount > 0) {
                listState.requestScrollToItem(itemCount - 1)
            }
        }

        // Error handling
        val errorMessage = remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            conversationModel.chatService.errorFlow.collect { error ->
                errorMessage.value = error.message
                snackbarState.showSnackbar(
                    message = "Error: ${error.message}",
                    duration = SnackbarDuration.Short
                )
            }
        }

        // Vibration on AI response
        val context = LocalContext.current
        var lastMessageCount by remember { mutableIntStateOf(0) }
        var lastMessageWasUser by remember { mutableStateOf(false) }

        LaunchedEffect(conversation?.nodes) {
            val nodes = conversation?.nodes ?: return@LaunchedEffect
            val currentCount = nodes.size

            if (currentCount > lastMessageCount) {
                val lastMessage = nodes.lastOrNull()?.messages?.lastOrNull()

                if (lastMessage?.role == MessageRole.ASSISTANT && lastMessageWasUser) {
                    Log.d("ChatScreen", "AI responded - vibrating")
                    VibrationHelper.vibrateOnAIResponse(context)
                }

                lastMessageWasUser = lastMessage?.role == MessageRole.USER
            }

            lastMessageCount = currentCount
        }

        // Loading and retry logic
        val isLoading = remember {
            derivedStateOf {
                conversationJobs.value.values.any { it != null }
            }
        }

        var isRetrying by remember { mutableStateOf(false) }

        LaunchedEffect(conversation?.nodes?.lastOrNull(), isLoading.value) {
            val lastMessage = conversation?.nodes?.lastOrNull()?.messages?.lastOrNull()

            if (lastMessage?.role == MessageRole.USER && !isLoading.value) {
                delay(3000)

                val stillWaiting = conversation?.nodes?.lastOrNull()?.messages?.lastOrNull()?.role == MessageRole.USER
                isRetrying = stillWaiting && !isLoading.value
            } else {
                isRetrying = false
            }
        }

        // Rest of your Scaffold and UI code remains the same...
        Scaffold(
            Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .reportable(settings.isShakable)
                .cloudy(enabled = bottomNav.isVisible || expanded.value || optionsSheet.value || showModelSwitcher.value),
            topBar = {
                TopAppBar(
                    {
                        Text(
                            text = conversation?.title ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton({
                            navigator?.pop()
                        }) {
                            Icon(painterResource(CommonIcons.ARROW_LEFT), null)
                        }
                    },
                    actions = {
                        IconButton({ expanded.value = true }) {
                            Icon(painterResource(CommonIcons.MENU_DOTS), null)
                        }
                        ChatActionsMenu(
                            expanded,
                            assistant,
                            {
                                conversationModel.retitle(
                                    conversation ?: return@ChatActionsMenu,
                                    true
                                )
                                expanded.value = false
                            },
                            {
                                val currentJobId = jobCounter.intValue++
                                deletedChat.add(Pair(conversation ?: return@ChatActionsMenu, currentJobId))
                                val chatPair = deletedChat.firstOrNull { pair -> pair.second == currentJobId }
                                conversationModel.updateObservedConversation(conversation?.copy(nodes = emptyList()) ?: return@ChatActionsMenu)

                                scope.launch {
                                    snackbarState.currentSnackbarData?.dismiss()
                                    val result = snackbarState.showSnackbar(
                                        message = "${conversation?.title?.ifBlank { "Chat" }} cleared",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Long
                                    )

//                                    when (result) {
//                                        SnackbarResult.ActionPerformed -> {
//                                            chatPair?.first?.let { original ->
//                                                conversationModel.upsert(original)
//                                            }
//                                        }
//                                        SnackbarResult.Dismissed -> {
//                                            conversationModel.upsert(conversation ?: return@launch)
//                                        }
//                                    }
                                    if (result == SnackbarResult.ActionPerformed) {
                                        chatPair?.first?.let { original ->
                                            conversationModel.upsert(original)
                                        }
                                    }
                                    chatPair?.let { deletedChat.remove(it) }
                                }
                                expanded.value = false
                            },
                            {
                                expanded.value = false
                                optionsSheet.value = true
                            }
                        )
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(10.dp, 5.dp)
                        .navigationBarsPadding()
                ) {
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(conversation?.suggestions ?: emptyList()) {
                            SuggestionChip(
                                { inputState.setMessageText(it) },
                                {
                                    Text(it, style = MaterialTheme.typography.labelLarge)
                                },
                                Modifier.padding(3.dp),
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        }
                        if(conversation?.suggestions?.isNotEmpty() == true) {
                            item {
                                SuggestionChip(
                                    {
                                        conversationModel.upsert(conversation?.copy(suggestions = emptyList()) ?: return@SuggestionChip)
                                    },
                                    {
                                        Icon(painterResource(CommonIcons.CROSS_SMALL), null)
                                    },
                                    Modifier.padding(3.dp),
                                    shape = CircleShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                            }
                        }
                    }

                    MediaFileInputRow(inputState)

                    InputBar(
                        isLoading,
                        model,
                        models,
                        inputState,
                        showModelSwitcher,
                        {
                            if (inputState.isEmpty().not()) {
                                scope.launch {
                                    conversationModel.handleMessageSend(inputState.getContents())
                                    listState.animateScrollToItem(conversation?.nodes?.size ?: 0)
                                }.invokeOnCompletion {
                                    keyboardController?.hide()
                                    inputState.clearInput()
                                }
                            }
                        },
                        {
//                            navigator?.push(ModelsScreen())
//                            modelsSheet.value = true
                            navigator?.push(ProvidersScreen())
                        },
                        {
                            optionsSheet.value = true
                        },
                        {
                            conversationJobs.value.values.map { it?.cancel() }
                        },
                        {
                            assistantModel.upsert(
                                assistant?.copy(currentModelUid = it.uid) ?: return@InputBar
                            )
                        }
                    )
                }
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
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = paddingValues.calculateTopPadding(),
                    bottom = 8.dp
                ),
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
            ) {
                itemsIndexed(
                    items = conversation?.nodes ?: emptyList(),
                    key = { _, node -> node.id }
                ) { index, node ->
                    node.messages.forEach { message ->
                        val isUser = message.role == MessageRole.USER
                        val isLastNode = index == (conversation?.nodes?.size ?: 0) - 1

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(
                                spring(stiffness = Spring.StiffnessLow)
                            ) { it / 2 }
                        ) {
                            ChatBubble(
                                model ?: return@AnimatedVisibility,
                                message.parts,
                                isUser,
                                (isLoading.value || isRetrying) && isLastNode,
                            )
                        }
                    }
                }
                // Void.
                item {
                    Spacer(Modifier.height(200.dp))
                }
            }

            if (optionsSheet.value) {
                ModalBottomSheet(
                    onDismissRequest = {
                        optionsSheet.value = false
                    },
                    sheetState = optionsState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Options(provider, inputState)
                }
            }

            if (modelsSheet.value) {
                ModalBottomSheet(
                    onDismissRequest = {
                        modelsSheet.value = false
                    },
                    sheetState = modelsState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    FullModels(getScreenModel(), getScreenModel(), getScreenModel())
                }
            }
        }
    }

    @Composable
    private fun ChatBubble(
        model: Model,
        parts: List<UIMessagePart>,
        isUser: Boolean,
        isProcessing: Boolean
    ) {
        val context = LocalContext.current
        val navigator = LocalNavigator.current

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            //Text.
            val text = parts.filterIsInstance<UIMessagePart.Text>()
            if (text.isNotEmpty()) {
                text.fastForEach { txt ->
                    Surface(
                        modifier = Modifier
                            .widthIn(max = if (isUser) 300.dp else Dp.Unspecified),
                        shape = RoundedCornerShape(
                            topStart = if (isUser) 20.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        ),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        //Text.
                        SelectionContainer {
                            MarkdownBlock(txt.text, Modifier.padding(8.dp, 8.dp))
                        }
                    }
                }
            }

            //Images.
            val images = parts.filterIsInstance<UIMessagePart.Image>()
            if (images.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    images.fastForEach {
                        AsyncImage(
                            model = it.url,
                            contentDescription = null,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .height(75.dp)
                        )
                    }
                }
            }

            // Documents
            val documents = parts.filterIsInstance<UIMessagePart.Document>()
            if (documents.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    documents.fastForEach {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    it.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (it.mime) {
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                                            Icon(
                                                painter = painterResource(CommonIcons.TEXT/*doc*/),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        "application/pdf" -> {
                                            Icon(
                                                painter = painterResource(CommonIcons.TEXT/*pdf*/),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        else -> {
                                            Icon(
                                                painterResource(CommonIcons.TEXT/*file*/),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = it.fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .widthIn(max = 200.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Videos
            val videos = parts.filterIsInstance<UIMessagePart.Video>()
            if (videos.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    videos.fastForEach {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    it.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center){
                                Icon(painterResource(CommonIcons.FILM), null)
                            }
                        }
                    }
                }
            }

            // Audios
            val audios = parts.filterIsInstance<UIMessagePart.Audio>()
            if (audios.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    audios.fastForEach {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    it.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        painterResource(CommonIcons.WAVEFORM),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            //Logo
            val logos = parts.filterIsInstance<UIMessagePart.Logo>()
            if (logos.isNotEmpty()) {
                logos.fastForEach {
                    val model = JsonInstant.decodeFromString<Model>(it.model ?: model.toString())

                    Row(Modifier.fillMaxWidth(), Arrangement.Start) {
                        Avatar(
                            isLoading = isProcessing,
                            logo = model.logoUrl,
                            modelName = model.displayName
                        ) {
                            navigator?.push(ModelConfigurationScreen(model))
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun InputBar(
        loading: State<Boolean>,
        model: Model?,
        models: List<Model>,              // NEW — full list for the switcher
        inputState: ChatInputState,
        modelSwitcher: MutableState<Boolean>,
        onSend: () -> Unit,
        onAttachClick: () -> Unit,
        onOptions: () -> Unit,
        onCancel: () -> Unit,
        onModelSelect: (Model) -> Unit    // NEW — called when user picks a model
    ) {
        val context = LocalContext.current
        var isFocused by remember { mutableStateOf(false) }

        val elevation by animateFloatAsState(
            targetValue = if (isFocused) 8f else 2f,
            label = "elevation"
        )
        val receiveContentListener = remember {
            ReceiveContentListener { transferableContent ->
                when {
                    transferableContent.hasMediaType(MediaType.Image) -> {
                        transferableContent.consume { item ->
                            val uri = item.uri
                            if (uri != null) {
                                inputState.addImages(
                                    context.createChatFilesByContents(listOf(uri))
                                )
                            }
                            uri != null
                        }
                    }
                    else -> transferableContent
                }
            }
        }

        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = elevation.dp,
                tonalElevation = 2.dp
            ) {
                TextField(
                    state = inputState.textContent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .contentReceiver(receiveContentListener)
                        .onFocusChanged { isFocused = it.isFocused }
                        .navigationBarsPadding()
                        .imePadding(),
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    ),
                    placeholder = {
                        Text(
                            text = model?.let {
                                "Ask ${it.displayName.split(' ', '/', '-', ',').firstOrNull()}"
                            } ?: "<- Add AI model",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.onSurface
                    ),
                    leadingIcon = if (inputState.getMessageText().isEmpty()) {
                        {
                            if (model == null) {
                                IconButton({
                                    modelSwitcher.value = true
                                }) {
                                    Icon(painterResource(CommonIcons.BOX_ADD), null)
                                }
                            } else {
                                Avatar(
                                    isLoading = loading.value,
                                    logo = model.logoUrl,
                                    modelName = model.displayName,
                                    onClick = {
                                        if (models.size > 1) modelSwitcher.value = true
                                        else onAttachClick()
                                    }
                                )
                            }
                        }
                    } else null,
                    trailingIcon = {
                        when {
                            loading.value -> IconButton(onCancel, Modifier.padding(5.dp)) {
                                Icon(painterResource(CommonIcons.CROSS_18), "cancel")
                            }
                            inputState.getMessageText().isEmpty() -> IconButton(onOptions, Modifier.padding(5.dp)) {
                                Icon(
                                    painterResource(CommonIcons.CLIP),
                                    "options",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            else -> IconButton(onSend, Modifier.padding(5.dp)) {
                                Icon(
                                    painterResource(CommonIcons.ARROW_UP),
                                    "send",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }

            // Gmail-style model switcher popup, anchored above the avatar
            if (modelSwitcher.value) {
                ModelSwitcherPopup(
                    models = models,
                    currentModel = model,
                    onDismiss = { modelSwitcher.value = false },
                    onModelSelect = { model ->
                        onModelSelect(model)
                        modelSwitcher.value = false
                    },
                    onAddModel = {
                        modelSwitcher.value = false
                        onAttachClick()
                    }
                )
            }
        }
    }
}