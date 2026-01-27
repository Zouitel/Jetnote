package zouitel.neighborhood.assistant.ui.conversation

import android.util.Log
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import zouitel.neighborhood.ai.core.MessageRole
import zouitel.neighborhood.ai.ui.UIMessage
import zouitel.neighborhood.ai.ui.UIMessagePart
import zouitel.neighborhood.ai.ui.isEmptyInputMessage
import zouitel.neighborhood.domain.usecase.AssistantUseCase
import zouitel.neighborhood.domain.usecase.ConversationUseCase
import zouitel.neighborhood.assistant.mapper.AssistantMapper
import zouitel.neighborhood.assistant.mapper.ConversationMapper
import zouitel.neighborhood.assistant.model.Assistant
import zouitel.neighborhood.assistant.model.AssistantAffectScope
import zouitel.neighborhood.assistant.model.Conversation
import zouitel.neighborhood.assistant.service.ChatService
import zouitel.neighborhood.assistant.utils.replaceRegexes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

private const val TAG = "ChatScreenModel"

class ChatScreenModel(
    private val _observeAll_: ConversationUseCase.ObserveAll,
    private val _observeById_: ConversationUseCase.ObserveById,
    private val _upsert: ConversationUseCase.Upsert,
    private val _delete: ConversationUseCase.Delete,
    private val mapper: ConversationMapper,
    val chatService: ChatService,
    private val _assistant_: AssistantUseCase.Observe,
    private val assistantMapper: AssistantMapper
): ScreenModel {

    // 🔥 CRITICAL FIX: Store the active conversation ID
    private var activeConversationId: Uuid? = null
        private set

    private val _observeAll = MutableStateFlow<List<Conversation>>(emptyList())
    val observeAll = _observeAll.stateIn(
        screenModelScope,
        SharingStarted.Lazily,
        emptyList()
    )

    val _observeById = MutableStateFlow<Conversation?>(null)
    val observeById = _observeById.stateIn(
        screenModelScope,
        SharingStarted.Lazily,
        null
    )

    private val _conversationJob = MutableStateFlow<Job?>(null)
    val conversationJob = _conversationJob.stateIn(
        screenModelScope,
        SharingStarted.Eagerly,
        null
    )

    val conversationJobs = chatService
        .getConversationJobs()
        .stateIn(
            screenModelScope,
            SharingStarted.Eagerly,
            emptyMap()
        )

    private val _assistant = MutableStateFlow<Assistant?>(null)
    private val _autoRetryJob = MutableStateFlow<Job?>(null)

    init {
        screenModelScope.launch {
            _observeAll_().collect {
                _observeAll.value = mapper.fromDomain(it)
            }
        }

        screenModelScope.launch(Dispatchers.IO) {
            _assistant_().collect {
                _assistant.value = assistantMapper.fromDomain(it)
            }
        }
    }

    suspend fun initConversationId(id: Uuid) {
        activeConversationId = id

        // 🔥 FIX: Don't use LaunchedEffect scope, use screenModelScope
        // This keeps it active even when navigating away
        screenModelScope.launch {
            _observeById_(id.toString()).collect { domainConv ->
                // Only update if this is still the active conversation
                if (activeConversationId == id) {
                    val uiConv = mapper.fromDomain(domainConv)
                    _observeById.value = uiConv

                    Log.d(
                        "ChatScreenModel",
                        "Conversation updated: ${uiConv.nodes.size} nodes, " +
                                "last role: ${uiConv.nodes.lastOrNull()?.messages?.lastOrNull()?.role}"
                    )
                }
            }
        }

        // Also observe generation jobs
        screenModelScope.launch {
            chatService.getGenerationJobStateFlow(id).collect { job ->
                if (activeConversationId == id) {
                    _conversationJob.value = job
                }
            }
        }
    }

    suspend fun initConversationIdService(id: Uuid) {
        if (activeConversationId == id) {
            chatService.initializeConversation(id)
        }
    }

    fun upsert(conversation: Conversation) {
        screenModelScope.launch {
            try {
                _upsert(mapper.toDomain(conversation))
                Log.d("ChatScreenModel", "Upserted conversation: ${conversation.id}")
            } catch (e: Exception) {
                Log.e("ChatScreenModel", "Error upserting conversation", e)
            }
        }
    }

    // Check if a conversation exists in the database
    suspend fun conversationExists(id: Uuid): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val conv = _observeById_(id.toString()).first()
                val uiConv = mapper.fromDomain(conv)
                val exists = uiConv.title.isNotBlank() || uiConv.nodes.isNotEmpty()
                Log.d(TAG, "Conversation $id exists: $exists (title: ${uiConv.title}, nodes: ${uiConv.nodes.size})")
                exists
            } catch (e: Exception) {
                Log.e(TAG, "Error checking conversation existence", e)
                false
            }
        }
    }

    fun delete(uid: String) {
        screenModelScope.launch {
            Log.d(TAG, "Deleting conversation: $uid")
            _delete(uid)
        }
    }

    fun handleMessageSend(content: List<UIMessagePart>) {
        if (content.isEmptyInputMessage()) return

        val conversationId = observeById.value?.id

        if (conversationId == null) {
            Log.e("ChatScreenModel", "ERROR: No conversation ID available")
            return
        }

        Log.d("ChatScreenModel", "Sending message to: $conversationId")

        val assistant = _assistant.value
        val processedContent = content.map { part ->
            when (part) {
                is UIMessagePart.Text -> {
                    part.copy(
                        text = part.text.replaceRegexes(
                            assistant = assistant,
                            scope = AssistantAffectScope.USER,
                            visual = false
                        )
                    )
                }
                else -> part
            }
        }

        // Send the message
        chatService.sendMessage(conversationId, processedContent)

        // 🔥 Start auto-retry monitoring
        startAutoRetryMonitoring(conversationId)
    }

    // Update conversation in memory only (for UI changes without DB save)
    fun updateConversationInMemory(conversation: Conversation) {
        if (conversation.id == activeConversationId) {
            Log.d(TAG, "Updating conversation ${conversation.id} in memory only")
            _observeById.value = conversation
            chatService.updateConversation(conversation.id, conversation)
        }
    }

    fun retitle(conversation: Conversation, force: Boolean = false) {
        screenModelScope.launch {
            chatService.generateTitle(conversation.id, conversation, force)
        }
    }

    fun updateObservedConversation(conversation: Conversation) {
        _observeById.value = conversation
    }

    fun retryLastMessage() {
        screenModelScope.launch {
            val conversation = observeById.value ?: return@launch
            val lastNode = conversation.nodes.lastOrNull() ?: return@launch
            val lastMessage = lastNode.messages.lastOrNull() ?: return@launch

            if (lastMessage.role == MessageRole.USER) {
                Log.d("ChatScreenModel", "Retrying last message")
                chatService.regenerateAtMessage(conversation.id, lastMessage, regenerateAssistantMsg = false)
            }
        }
    }

    // Add these at the top of the class
    private val maxRetries = 3
    private val retryDelayMs = listOf(2000L, 5000L, 10000L) // 2s, 5s, 10s
    private val _retryAttempts = MutableStateFlow<Map<Uuid, Int>>(emptyMap())

    // Add this method for automatic retry
    private fun scheduleAutoRetry(conversationId: Uuid, message: UIMessage, attempt: Int = 0) {
        if (attempt >= maxRetries) {
            Log.e("ChatScreenModel", "Max retries reached for conversation $conversationId")
            return
        }

        screenModelScope.launch {
            val delay = retryDelayMs.getOrElse(attempt) { 10000L }
            Log.d("ChatScreenModel", "Scheduling retry #${attempt + 1} in ${delay}ms")

            delay(delay)

            // Check if we still need to retry (response might have arrived)
            val currentConv = observeById.value
            val lastMessage = currentConv?.nodes?.lastOrNull()?.messages?.lastOrNull()

            if (lastMessage?.role == MessageRole.USER && currentConv.id == conversationId) {
                Log.d("ChatScreenModel", "Auto-retrying message (attempt ${attempt + 1})")

                _retryAttempts.value = _retryAttempts.value.toMutableMap().apply {
                    put(conversationId, attempt + 1)
                }

                try {
                    chatService.regenerateAtMessage(conversationId, message, regenerateAssistantMsg = false)
                } catch (e: Exception) {
                    Log.e("ChatScreenModel", "Retry failed", e)
                    // Schedule next retry
                    scheduleAutoRetry(conversationId, message, attempt + 1)
                }
            } else {
                Log.d("ChatScreenModel", "Response received, canceling retry")
                _retryAttempts.value = _retryAttempts.value.toMutableMap().apply {
                    remove(conversationId)
                }
            }
        }
    }
    fun startAutoRetryMonitoring(conversationId: Uuid) {
        // Cancel any existing monitoring
        _autoRetryJob.value?.cancel()

        _autoRetryJob.value = screenModelScope.launch {
            // Wait 3 seconds after message is sent
            delay(3000)

            // Check if AI has responded
            val currentConv = observeById.value
            if (currentConv == null || currentConv.id != conversationId) {
                Log.d("ChatScreenModel", "Conversation changed, stopping retry")
                return@launch
            }

            val lastNode = currentConv.nodes.lastOrNull()
            val lastMessage = lastNode?.messages?.lastOrNull()

            // If last message is from user, AI hasn't responded yet
            if (lastMessage?.role == MessageRole.USER) {
                Log.d("ChatScreenModel", "No AI response after 3s, starting auto-retry...")

                // Retry up to 3 times with increasing delays
                repeat(3) { attempt ->
                    val delayTime = when (attempt) {
                        0 -> 0L      // First retry: immediate
                        1 -> 5000L   // Second retry: after 5s
                        2 -> 10000L  // Third retry: after 10s
                        else -> 10000L
                    }

                    delay(delayTime)

                    // Check again if we still need to retry
                    val conv = observeById.value
                    val lastMsg = conv?.nodes?.lastOrNull()?.messages?.lastOrNull()

                    if (lastMsg?.role == MessageRole.USER && conv?.id == conversationId) {
                        Log.d("ChatScreenModel", "Auto-retry attempt ${attempt + 1}/3")

                        // Retry by regenerating
                        try {
                            chatService.regenerateAtMessage(
                                conversationId,
                                lastMsg,
                                regenerateAssistantMsg = false
                            )

                            // Wait to see if this attempt works
                            delay(3000)

                            // Check if response arrived
                            val checkConv = observeById.value
                            val checkLast = checkConv?.nodes?.lastOrNull()?.messages?.lastOrNull()

                            if (checkLast?.role == MessageRole.ASSISTANT) {
                                Log.d("ChatScreenModel", "Retry successful!")
                                return@launch // Success, stop retrying
                            }
                        } catch (e: Exception) {
                            Log.e("ChatScreenModel", "Retry attempt ${attempt + 1} failed: ${e.message}")
                        }
                    } else {
                        Log.d("ChatScreenModel", "Response received, stopping retry")
                        return@launch // Response arrived, stop retrying
                    }
                }

                Log.e("ChatScreenModel", "All retry attempts exhausted")
            }
        }
    }
    // Expose retry attempts as state
    val retryAttempts = _retryAttempts.stateIn(
        screenModelScope,
        SharingStarted.Lazily,
        emptyMap()
    )
    // Stop retry monitoring
    fun stopAutoRetry() {
        _autoRetryJob.value?.cancel()
        _autoRetryJob.value = null
    }
    override fun onDispose() {
        super.onDispose()
        Log.d(TAG, "Disposing ChatScreenModel for conversation: $activeConversationId")
        activeConversationId?.let { id ->
            chatService.removeConversationReference(id)
        }
        activeConversationId = null
    }
}