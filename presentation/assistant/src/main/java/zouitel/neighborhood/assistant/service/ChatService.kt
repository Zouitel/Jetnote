package zouitel.neighborhood.assistant.service

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import zouitel.neighborhood.ai.core.MessageRole
import zouitel.neighborhood.ai.provider.ProviderManager
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.ai.provider.TextGenerationParams
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.ai.ui.finishReasoning
import zouitel.neighborhood.ai.ui.truncate
import zouitel.neighborhood.assistant.AppScope
import zouitel.neighborhood.assistant.AssistantActivity
import zouitel.neighborhood.assistant.Constants.CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID
import zouitel.neighborhood.assistant.ai.GenerationChunk
import zouitel.neighborhood.assistant.ai.GenerationHandler
import zouitel.neighborhood.assistant.ai.prompts.DEFAULT_SUGGESTION_PROMPT
import zouitel.neighborhood.assistant.ai.prompts.DEFAULT_TITLE_PROMPT
import zouitel.neighborhood.assistant.ai.transformers.Base64ImageToLocalFileTransformer
import zouitel.neighborhood.assistant.ai.transformers.DocumentAsPromptTransformer
import zouitel.neighborhood.assistant.ai.transformers.OcrTransformer
import zouitel.neighborhood.assistant.ai.transformers.PlaceholderTransformer
import zouitel.neighborhood.assistant.ai.transformers.RegexOutputTransformer
import zouitel.neighborhood.assistant.ai.transformers.TemplateTransformer
import zouitel.neighborhood.assistant.ai.transformers.ThinkTagTransformer
import zouitel.neighborhood.domain.usecase.AssistantUseCase
import zouitel.neighborhood.domain.usecase.ConversationUseCase
import zouitel.neighborhood.domain.usecase.ModelUseCase
import zouitel.neighborhood.domain.usecase.ProviderUseCase
import zouitel.neighborhood.assistant.mapper.AssistantMapper
import zouitel.neighborhood.assistant.mapper.ConversationMapper
import zouitel.neighborhood.assistant.mapper.ModelMapper
import zouitel.neighborhood.assistant.mapper.ProviderMapper
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.model.Conversation
import zouitel.neighborhood.assistant.utils.applyPlaceholders
import zouitel.neighborhood.assistant.utils.deleteChatFiles
import zouitel.neighborhood.assistant.utils.toMessageNode
import zouitel.neighborhood.logic.Logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.uuid.Uuid
import zouitel.neighborhood.ai.provider.Model as ProviderModel

private const val TAG = "ChatService"

private val inputTransformers by lazy {
    listOf(
        PlaceholderTransformer,
        DocumentAsPromptTransformer,
        OcrTransformer
    )
}

private val outputTransformers by lazy {
    listOf(
        ThinkTagTransformer,
        Base64ImageToLocalFileTransformer,
        RegexOutputTransformer
    )
}
@OptIn(ExperimentalCoroutinesApi::class)
class ChatService(
    private val context: Application,
    private val appScope: AppScope,
    private val conversationMapper: ConversationMapper,
    private val generationHandler: GenerationHandler,
    private val templateTransformer: TemplateTransformer,
    private val modelMapper: ModelMapper,
    private val observeProviders: ProviderUseCase.ObserveAll,
    private val providerMapper: ProviderMapper,
    private val providerManager: ProviderManager,
    private val observeAssistant: AssistantUseCase.Observe,
    private val assistantMapper: AssistantMapper,
    private val upsertConversation: ConversationUseCase.Upsert,
    private val observeConversationsById: ConversationUseCase.ObserveById,
    private val observeModelById: ModelUseCase.ObserveById
    ) {
    private val _model = MutableStateFlow<ProviderModel?>(null)
    val currentModel: ProviderModel? get() = _model.value

    private val _providers = MutableStateFlow<List<ProviderSetting>>(emptyList())
    val providers: List<ProviderSetting> get() = _providers.value

    private val _assistant = MutableStateFlow<Assistant?>(null)
    val assistant: Assistant? get() = _assistant.value
    // Track model observer job so we can restart it
    private var modelObserverJob: Job? = null

    init {
        Log.d(TAG, "=== Initializing ChatService ===")

        // Observe providers
        appScope.launch {
            observeProviders()
                .catch { e ->
                    Log.e(TAG, "Error observing providers", e)
                }
                .collect { domainProviders ->
                    _providers.value = providerMapper.fromDomain(domainProviders)
                    Log.d(TAG, "Providers updated: ${_providers.value.size} providers")
                }
        }

        // Observe assistant
        appScope.launch(Dispatchers.IO) {
            observeAssistant()
                .catch { e ->
                    Log.e(TAG, "Error observing assistant", e)
                }
                .collect { domainAssistant ->
                    val assistant = assistantMapper.fromDomain(domainAssistant)
                    _assistant.value = assistant
                    Log.d(TAG, "Assistant updated: modelUid = ${assistant?.currentModelUid}")
                }
        }

        // 🔥 This automatically reacts when assistant.currentModelUid changes
        appScope.launch(Dispatchers.IO) {
            _assistant
                .filterNotNull()
                .flatMapLatest { assistant ->
                    val modelUid = assistant.currentModelUid
                    if (modelUid != null) {
                        Log.d(TAG, "Observing model: $modelUid")
                        observeModelById(modelUid)
                            .catch { e ->
                                Log.e(TAG, "Error observing model $modelUid", e)
                                emit(null)
                            }
                    } else {
                        Log.w(TAG, "No model UID")
                        flowOf(null)
                    }
                }
                .collect { domainModel ->
                    val model = domainModel?.let { modelMapper.fromDomain(it) }
                    _model.value = model
                    Log.d(TAG, "Model updated: ${model?.displayName ?: "null"}")
                }
        }
    }

    // 🔥 NEW: Restart model observer when model changes
    private fun restartModelObserver(modelUid: String) {
        // Cancel existing observer
        modelObserverJob?.cancel()

        // Start new observer
        modelObserverJob = appScope.launch(Dispatchers.IO) {
            Log.d(TAG, "Starting model observer for: $modelUid")

            try {
                observeModelById(modelUid).collect { domainModel ->
                    val newModel = modelMapper.fromDomain(domainModel)
                    _model.value = newModel
                    Log.d(TAG, "Model updated: ${newModel.displayName} (provider: ${newModel.providerId})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing model $modelUid", e)
            }
        }
    }

    private val conversations = ConcurrentHashMap<Uuid, MutableStateFlow<Conversation>>()

    // 记录哪些conversation有VM引用
    private val conversationReferences = ConcurrentHashMap<Uuid, Int>()

    // 存储每个对话的生成任务状态
    private val _generationJobs = MutableStateFlow<Map<Uuid, Job?>>(emptyMap())
    private val generationJobs: StateFlow<Map<Uuid, Job?>> = _generationJobs
        .asStateFlow()

    // 错误流
    private val _errorFlow = MutableSharedFlow<Throwable>()
    val errorFlow: SharedFlow<Throwable> = _errorFlow.asSharedFlow()

    // 生成完成流
    private val _generationDoneFlow = MutableSharedFlow<Uuid>()
    val generationDoneFlow: SharedFlow<Uuid> = _generationDoneFlow.asSharedFlow()

    // 前台状态管理
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> _isForeground.value = true
            Lifecycle.Event.ON_STOP -> _isForeground.value = false
            else -> {}
        }
    }

    init {
        // Add a lifecycle observer
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
    }

    fun cleanup() = runCatching {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(lifecycleObserver)
        _generationJobs.value.values.forEach { it?.cancel() }
    }

    fun addConversationReference(conversationId: Uuid) {
        conversationReferences[conversationId] = conversationReferences.getOrDefault(conversationId, 0) + 1
        Log.d(
            TAG,
            "Added reference for $conversationId (current references: ${conversationReferences[conversationId] ?: 0})"
        )
    }

    fun removeConversationReference(conversationId: Uuid) {
        conversationReferences[conversationId]?.let { count ->
            if (count > 1) {
                conversationReferences[conversationId] = count - 1
            } else {
                conversationReferences.remove(conversationId)
            }
        }
        Log.d(
            TAG,
            "Removed reference for $conversationId (current references: ${conversationReferences[conversationId] ?: 0})"
        )
        appScope.launch {
            delay(500)
            checkAllConversationsReferences()
        }
    }

    // 检查是否有引用
    private fun hasReference(conversationId: Uuid): Boolean {
        return conversationReferences.containsKey(conversationId) || _generationJobs.value.containsKey(
            conversationId
        )
    }

    // Check the references of all conversations (call after the end of generation)
    fun checkAllConversationsReferences() {
        conversations.keys.forEach { conversationId ->
            if (!hasReference(conversationId)) {
                cleanupConversation(conversationId)
            }
        }
    }

    // Get the StateFlow of the conversation
    fun getConversationFlow(conversationId: Uuid): StateFlow<Conversation> {
        return conversations.getOrPut(conversationId) {
            MutableStateFlow(
                Conversation.ofId(id = conversationId)
            )
        }
    }

    // Get the build task status stream.
    fun getGenerationJobStateFlow(conversationId: Uuid): Flow<Job?> {
        return generationJobs.map { jobs -> jobs[conversationId] }
    }

    fun getConversationJobs(): Flow<Map<Uuid, Job?>> {
        return generationJobs
    }

    private fun setGenerationJob(conversationId: Uuid, job: Job?) {
        if (job == null) {
            removeGenerationJob(conversationId)
            return
        }
        _generationJobs.value = _generationJobs.value.toMutableMap().apply {
            this[conversationId] = job
        }.toMap() // 确保创建新的不可变Map实例
    }

    private fun getGenerationJob(conversationId: Uuid): Job? {
        return _generationJobs.value[conversationId]
    }

    private fun removeGenerationJob(conversationId: Uuid) {
        _generationJobs.value = _generationJobs.value.toMutableMap().apply {
            remove(conversationId)
        }.toMap() // 确保创建新的不可变Map实例
    }

    // Initialize conversation - FIXED to not overwrite existing conversations
    suspend fun initializeConversation(conversationId: Uuid) {
        if (conversations.containsKey(conversationId)) {
            Log.d(TAG, "Conversation $conversationId already initialized")

            // Just sync updates, don't overwrite
            observeConversationsById(conversationId.toString()).collect { domainConv ->
                val dbConv = conversationMapper.fromDomain(domainConv)
                val memoryConv = getConversationFlow(conversationId).value

                // Only update if DB has more content
                if (dbConv.nodes.size >= memoryConv.nodes.size) {
                    updateConversation(conversationId, dbConv)
                }
            }
        } else {
            Log.d(TAG, "First initialization of conversation $conversationId")

            observeConversationsById(conversationId.toString()).collect { domainConv ->
                updateConversation(conversationId, conversationMapper.fromDomain(domainConv))
            }
        }
    }

    fun sendMessage(conversationId: Uuid, content: List<UIMessagePart>) {
        if (!conversations.containsKey(conversationId)) {
            Log.e(TAG, "ERROR: Conversation not initialized: $conversationId")
            appScope.launch {
                initializeConversation(conversationId)
                delay(200)
                sendMessageInternal(conversationId, content)
            }
            return
        }

        sendMessageInternal(conversationId, content)
    }

    private fun sendMessageInternal(conversationId: Uuid, content: List<UIMessagePart>) {
        getGenerationJob(conversationId)?.cancel()

        val job = appScope.launch {
            try {
                val currentConversation = getConversationFlow(conversationId).value

                val userMessage = UIMessage(
                    role = MessageRole.USER,
                    parts = content,
                )

                val newConversation = currentConversation.copy(
                    nodes = currentConversation.nodes + userMessage.toMessageNode(),
                )

                // 🔥 Save immediately so message is never lost
                saveConversation(conversationId, newConversation)
                Log.d(TAG, "User message saved to database")

                // Start AI completion
                handleMessageComplete(conversationId)

                _generationDoneFlow.emit(conversationId)

            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                _errorFlow.emit(e)

                // 🔥 On error, trigger auto-retry will be handled by ViewModel
            }
        }

        setGenerationJob(conversationId, job)
        job.invokeOnCompletion { exception ->
            setGenerationJob(conversationId, null)

            if (exception != null) {
                Log.e(TAG, "Generation job failed: ${exception.message}")
                // Error will trigger auto-retry in ViewModel
            }

            appScope.launch {
                delay(500)
                checkAllConversationsReferences()
            }
        }
    }

    // 重新生成消息
    fun regenerateAtMessage(
        conversationId: Uuid,
        message: UIMessage,
        regenerateAssistantMsg: Boolean = true
    ) {
        getGenerationJob(conversationId)?.cancel()

        val job = appScope.launch {
            try {
                val conversation = getConversationFlow(conversationId).value

                if (message.role == MessageRole.USER) {
                    // 如果是用户消息，则截止到当前消息
                    val node = conversation.getMessageNodeByMessage(message)
                    val indexAt = conversation.nodes.indexOf(node)
                    val newConversation = conversation.copy(
                        nodes = conversation.nodes.subList(0, indexAt + 1)
                    )
                    saveConversation(conversationId, newConversation)
                    handleMessageComplete(conversationId)
                } else {
                    if (regenerateAssistantMsg) {
                        val node = conversation.getMessageNodeByMessage(message)
                        val nodeIndex = conversation.nodes.indexOf(node)
                        handleMessageComplete(conversationId, messageRange = 0..<nodeIndex)
                    } else {
                        saveConversation(conversationId, conversation)
                    }
                }

                _generationDoneFlow.emit(conversationId)
            } catch (e: Exception) {
                _errorFlow.emit(e)
            }
        }

        setGenerationJob(conversationId, job)
        job.invokeOnCompletion {
            setGenerationJob(conversationId, null)
            // 取消生成任务后，检查是否有其他任务在进行
            appScope.launch {
                delay(500)
                checkAllConversationsReferences()
            }
        }
    }

    private suspend fun handleMessageComplete(
        conversationId: Uuid,
        messageRange: ClosedRange<Int>? = null
    ) {
        runCatching {
            val conversation = getConversationFlow(conversationId).value

            // reset suggestions
            updateConversation(conversationId, conversation.copy(suggestions = emptyList()))

            // memory tool
//            if (!currentModel.abilities.contains(ModelAbility.TOOL)) {
//                if (settings.enableWebSearch || mcpManager.getAllAvailableTools().isNotEmpty()) {
//                    _errorFlow.emit(IllegalStateException(context.getString(R.string.tools_warning)))
//                }
//            }

            // check invalid messages
            checkInvalidMessages(conversationId)
            // start generating
            generationHandler.generateText(
                currentModel = currentModel ?: error("currentModel is null"),
                providers = providers,
                messages = conversation.currentMessages.let {
                    if (messageRange != null) {
                        it.subList(messageRange.start, messageRange.endInclusive + 1)
                    } else {
                        it
                    }
                },
                assistant = assistant,
                inputTransformers = buildList {
                    addAll(inputTransformers)
                    add(templateTransformer)
                },
                outputTransformers = outputTransformers,
                tools = buildList {
//                    if (settings.enableWebSearch) {
//                        add(createSearchTool(settings))
//                    }
//                    addAll(localTools.getTools(settings.getCurrentAssistant().localTools))
//                    mcpManager.getAllAvailableTools().forEach { tool ->
//                        add(
//                            Tool(
//                                name = tool.name,
//                                description = tool.description ?: "",
//                                parameters = { tool.inputSchema },
//                                execute = {
//                                    mcpManager.callTool(tool.name, it.jsonObject)
//                                },
//                            )
//                        )
//                    }
                },
                truncateIndex = conversation.truncateIndex,
            ).onCompletion {
                // It may have been canceled, or ended unexpectedly, and it was updated
                val updatedConversation = getConversationFlow(conversationId).value.copy(
                    nodes = getConversationFlow(conversationId).value.nodes.map { node ->
                        node.copy(messages = node.messages.map { it.finishReasoning() })
                    }
                )
                updateConversation(conversationId, updatedConversation)

                // Show notification if app is not in foreground
//                if (!isForeground.value && settings.displaySetting.enableNotificationOnMessageGeneration) {
//                    sendGenerationDoneNotification(conversationId)
//                }
            }.collect { chunk ->
                when (chunk) {
                    is GenerationChunk.Messages -> {
                        val updatedConversation = getConversationFlow(conversationId).value
                            .updateCurrentMessages(chunk.messages)
                        updateConversation(conversationId, updatedConversation)
                    }
                }
            }
        }.onFailure {
            it.printStackTrace()
            _errorFlow.emit(it)
            Logging.log(TAG, "handleMessageComplete: $it")
            Logging.log(TAG, it.stackTraceToString())
        }.onSuccess {
            val finalConversation = getConversationFlow(conversationId).value
            saveConversation(conversationId, finalConversation)

            addConversationReference(conversationId)
            appScope.launch {
                coroutineScope {
                    launch { generateTitle(conversationId, finalConversation) }
                    launch { generateSuggestion(conversationId, finalConversation) }
                }
            }.invokeOnCompletion {
//                removeConversationReference(conversationId)
            }
        }
    }

    // 检查无效消息
    private fun checkInvalidMessages(conversationId: Uuid) {
        val conversation = getConversationFlow(conversationId).value
        var messagesNodes = conversation.nodes

        // 移除无效tool call
        messagesNodes = messagesNodes.mapIndexed { index, node ->
            val next = if (index < messagesNodes.size - 1) messagesNodes[index + 1] else null
            if (node.currentMessage.hasPart<UIMessagePart.ToolCall>()) {
                if (next?.currentMessage?.hasPart<UIMessagePart.ToolResult>() != true) {
                    return@mapIndexed node.copy(
                        messages = node.messages.filter { it.id != node.currentMessage.id },
                        selectIndex = node.selectIndex - 1
                    )
                }
            }
            node
        }

        // 更新index
        messagesNodes = messagesNodes.map { node ->
            if (node.messages.isNotEmpty() && node.selectIndex !in node.messages.indices) {
                node.copy(selectIndex = 0)
            } else {
                node
            }
        }

        // 移除无效消息
        messagesNodes = messagesNodes.filter { it.messages.isNotEmpty() }

        updateConversation(conversationId, conversation.copy(nodes = messagesNodes))
    }

    // Generate a title
    suspend fun generateTitle(conversationId: Uuid, conversation: Conversation, force: Boolean = false) {
        val shouldGenerate = when {
            force -> true
            conversation.title.isBlank() -> true
            else -> false
        }
        if (!shouldGenerate) return

        runCatching {
            val model = currentModel ?: error("Model not found")
            val provider = providers.find { it.id == model.providerId } ?: error("Provider not found")

            val providerHandler = providerManager.getProviderByType(provider)
            val result = providerHandler.generateText(
                providerSetting = provider,
                messages = listOf(
                    UIMessage.user(
                        prompt = DEFAULT_TITLE_PROMPT.applyPlaceholders(
                            "locale" to Locale.getDefault().displayName,
                            "content" to conversation.currentMessages.truncate(conversation.truncateIndex)
                                .joinToString("\n\n") { it.summaryAsText() })
                    ),
                ),
                params = TextGenerationParams(
                    model = model, temperature = 0.3f, thinkingBudget = 0
                ),
            )

            // After generation, the conversation may not be the latest, so it needs to be re-acquisition.
               observeConversationsById(conversationId.toString()).collect {
                conversationMapper.fromDomain(it).let {
                    saveConversation(
                        conversationId,
                        it.copy(title = result.choices[0].message?.toText()?.trim() ?: "")
                    )
                }
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    // Generate recommendations
    suspend fun generateSuggestion(conversationId: Uuid, conversation: Conversation) {
        runCatching {
            val model = currentModel ?: error("currentModel is null")
            val provider = providers.find { it.id == currentModel?.providerId } ?: error("providers is null")

            updateConversation(
                conversationId,
                getConversationFlow(conversationId).value.copy(suggestions = emptyList())
            )

            val providerHandler = providerManager.getProviderByType(provider)
            val result = providerHandler.generateText(
                providerSetting = provider,
                messages = listOf(
                    UIMessage.user(
                        DEFAULT_SUGGESTION_PROMPT.applyPlaceholders(
                            "locale" to Locale.getDefault().displayName,
                            "content" to conversation.currentMessages.truncate(conversation.truncateIndex)
                                .takeLast(8).joinToString("\n\n") { it.summaryAsText() }),
                    )
                ),
                params = TextGenerationParams(
                    model = model,
                    temperature = 1.0f,
                    thinkingBudget = 0,
                ),
            )
            val suggestions =
                result.choices[0].message?.toText()?.split("\n")?.map { it.trim() }
                    ?.filter { it.isNotBlank() } ?: emptyList()

            saveConversation(
                conversationId,
                getConversationFlow(conversationId).value.copy(
                    suggestions = suggestions.take(
                        10
                    )
                )
            )
        }.onFailure {
            it.printStackTrace()
        }
    }

    // 发送生成完成通知
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendGenerationDoneNotification(conversationId: Uuid) {
        val conversation = getConversationFlow(conversationId).value
        val notification =
            NotificationCompat.Builder(context, CHAT_COMPLETED_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("")
                .setContentText(conversation.currentMessages.lastOrNull()?.toText()?.take(50) ?: "")
//                .setSmallIcon(R.drawable.small_icon)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setContentIntent(getPendingIntent(context, conversationId))

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(1, notification.build())
    }

    private fun getPendingIntent(context: Context, conversationId: Uuid): PendingIntent {
        val intent = Intent(context, AssistantActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("conversationId", conversationId.toString())
        }
        return PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun updateConversation(conversationId: Uuid, conversation: Conversation) {
        if (conversation.id != conversationId) return
        checkFilesDelete(conversation, getConversationFlow(conversationId).value)
        conversations.getOrPut(conversationId) { MutableStateFlow(conversation) }.value =
            conversation
    }

    // 检查文件删除
    private fun checkFilesDelete(newConversation: Conversation, oldConversation: Conversation) {
        val newFiles = newConversation.files
        val oldFiles = oldConversation.files
        val deletedFiles = oldFiles.filter { file ->
            newFiles.none { it == file }
        }
        if (deletedFiles.isNotEmpty()) {
            context.deleteChatFiles(deletedFiles)
            Log.w(TAG, "checkFilesDelete: $deletedFiles")
        }
    }

    suspend fun saveConversation(conversationId: Uuid, conversation: Conversation) {
        //If the conversation is empty, it is not saved
        if (conversation.title.isBlank() && conversation.nodes.isEmpty()) return

        val updatedConversation = conversation.copy()
        updateConversation(conversationId, updatedConversation)

        try {
            upsertConversation(conversationMapper.toDomain(updatedConversation))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cleanupConversation(conversationId: Uuid) {
        val conversation = getConversationFlow(conversationId).value

        // 🔥 Don't cleanup if conversation has content
        if (conversation.nodes.isNotEmpty() || conversation.title.isNotBlank()) {
            Log.d(TAG, "Not cleaning up conversation $conversationId - has content (${conversation.nodes.size} nodes)")
            return
        }

        getGenerationJob(conversationId)?.cancel()
        removeGenerationJob(conversationId)
        conversations.remove(conversationId)

        Log.i(TAG, "Cleaned up empty conversation: $conversationId")
    }
}
