package com.gemmaheretic.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gemmaheretic.app.data.local.preferences.AppPreferences
import com.gemmaheretic.app.data.repository.ChatRepository
import com.gemmaheretic.app.data.repository.EndpointRepository
import com.gemmaheretic.app.domain.model.*
import com.gemmaheretic.app.network.api.OllamaChatMessage
import com.gemmaheretic.app.network.api.OllamaChatRequest
import com.gemmaheretic.app.network.api.OllamaOptions
import com.gemmaheretic.app.network.streaming.StreamingChatClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatUiState(
    val session: ChatSession? = null,
    val messages: List<ChatMessage> = emptyList(),
    val streamState: StreamState = StreamState.Idle,
    val currentStreamContent: String = "",
    val endpoint: Endpoint? = null,
    val models: List<OllamaModel> = emptyList(),
    val inputText: String = "",
    val isLoadingModels: Boolean = false,
    val error: String? = null,
    val showSettings: Boolean = false
)

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val endpointRepository: EndpointRepository,
    private val preferences: AppPreferences,
    private val sessionId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var titleGenerated = false
    private val streamingClient = StreamingChatClient()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            val session = chatRepository.getSession(sessionId) ?: return@launch
            val endpoint = endpointRepository.getEndpointById(session.endpointId)

            _uiState.update { it.copy(session = session, endpoint = endpoint) }

            // Observe messages
            chatRepository.getMessagesForSession(sessionId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun loadModels() {
        val endpoint = _uiState.value.endpoint ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            val result = endpointRepository.fetchModels(endpoint.url)
            result.onSuccess { models ->
                _uiState.update { it.copy(models = models, isLoadingModels = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingModels = false, error = "Failed to load models: ${e.message}") }
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return
        if (_uiState.value.streamState == StreamState.Streaming) return

        _uiState.update { it.copy(inputText = "") }
        sendUserMessage(text)
    }

    fun editAndResend(messageId: Long, newContent: String) {
        viewModelScope.launch {
            val messages = _uiState.value.messages
            val targetMsg = messages.find { it.id == messageId } ?: return@launch

            chatRepository.deleteMessagesAfter(sessionId, targetMsg.createdAt)
            sendUserMessage(newContent)
        }
    }

    fun regenerate() {
        viewModelScope.launch {
            val messages = _uiState.value.messages
            if (messages.isEmpty()) return@launch

            val lastMsg = messages.last()
            if (lastMsg.role == MessageRole.ASSISTANT) {
                chatRepository.deleteMessage(lastMsg.id)
            }

            val currentMessages = chatRepository.getMessagesOnce(sessionId)
            if (currentMessages.isNotEmpty()) {
                generateResponse(currentMessages)
            }
        }
    }

    fun stopGeneration() {
        streamJob?.cancel()
        streamingClient.cancelAll()
        streamJob = null
        viewModelScope.launch {
            val content = _uiState.value.currentStreamContent
            if (content.isNotEmpty()) {
                chatRepository.addMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = MessageRole.ASSISTANT,
                        content = content
                    )
                )
            }
            _uiState.update { it.copy(streamState = StreamState.Idle, currentStreamContent = "") }
            chatRepository.touchSession(sessionId)
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearChat(sessionId)
            titleGenerated = false
        }
    }

    fun updateModel(modelName: String) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val updated = session.copy(modelName = modelName, updatedAt = System.currentTimeMillis())
            chatRepository.updateSession(updated)
            _uiState.update { it.copy(session = updated) }
            preferences.setLastModel(modelName)
        }
    }

    fun updateSessionConfig(config: ChatConfig) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val updated = session.copy(
                systemPrompt = config.systemPrompt,
                temperature = config.temperature,
                topP = config.topP,
                topK = config.topK,
                numCtx = config.numCtx,
                maxTokens = config.maxTokens,
                repeatPenalty = config.repeatPenalty,
                seed = config.seed,
                keepAlive = config.keepAlive,
                updatedAt = System.currentTimeMillis()
            )
            chatRepository.updateSession(updated)
            _uiState.update { it.copy(session = updated) }
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun sendUserMessage(text: String) {
        viewModelScope.launch {
            val userMessage = ChatMessage(
                sessionId = sessionId,
                role = MessageRole.USER,
                content = text
            )
            chatRepository.addMessage(userMessage)
            chatRepository.touchSession(sessionId)

            val allMessages = chatRepository.getMessagesOnce(sessionId)
            val shouldTitle = !titleGenerated && allMessages.size <= 1
            generateResponse(allMessages, if (shouldTitle) text else null)
        }
    }

    private fun generateResponse(messages: List<ChatMessage>, titleSource: String? = null) {
        val session = _uiState.value.session ?: return
        val endpoint = _uiState.value.endpoint ?: return

        val ollamaMessages = buildList {
            session.systemPrompt?.let { prompt ->
                if (prompt.isNotBlank()) {
                    add(OllamaChatMessage(role = "system", content = prompt))
                }
            }
            messages.forEach { msg ->
                add(OllamaChatMessage(role = msg.role.value, content = msg.content))
            }
        }

        // Only include options if at least one is set — avoids sending
        // an empty options object that could confuse Ollama
        val hasOptions = listOf(
            session.temperature, session.topP, session.topK,
            session.numCtx, session.maxTokens, session.repeatPenalty, session.seed
        ).any { it != null }

        val options = if (hasOptions) OllamaOptions(
            temperature = session.temperature,
            topP = session.topP,
            topK = session.topK,
            numCtx = session.numCtx,
            numPredict = session.maxTokens,
            repeatPenalty = session.repeatPenalty,
            seed = session.seed
        ) else null

        val request = OllamaChatRequest(
            model = session.modelName,
            messages = ollamaMessages,
            stream = true,
            options = options,
            keepAlive = session.keepAlive
        )

        _uiState.update { it.copy(streamState = StreamState.Streaming, currentStreamContent = "") }

        val startTime = System.currentTimeMillis()

        streamJob = viewModelScope.launch {
            val contentBuilder = StringBuilder()
            var lastEmitTime = 0L

            try {
                val result = withContext(Dispatchers.IO) {
                    streamingClient.streamBlocking(
                        baseUrl = endpoint.url,
                        request = request,
                        onToken = { token ->
                            contentBuilder.append(token)
                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime > 40) {
                                val snapshot = contentBuilder.toString()
                                _uiState.update { it.copy(currentStreamContent = snapshot) }
                                lastEmitTime = now
                            }
                        }
                    )
                }

                // Final flush — show any tokens buffered since last emit
                val finalContent = contentBuilder.toString()
                if (finalContent.isNotEmpty()) {
                    chatRepository.addMessage(
                        ChatMessage(
                            sessionId = sessionId,
                            role = MessageRole.ASSISTANT,
                            content = finalContent,
                            durationMs = System.currentTimeMillis() - startTime,
                            tokenCount = result.evalCount
                        )
                    )
                }
                _uiState.update {
                    it.copy(streamState = StreamState.Complete, currentStreamContent = "")
                }
                chatRepository.touchSession(sessionId)

                // Generate title AFTER response completes — never concurrent
                if (titleSource != null) {
                    generateTitle(titleSource)
                }

            } catch (e: Exception) {
                // Save partial content on error
                val partial = contentBuilder.toString()
                if (partial.isNotEmpty()) {
                    chatRepository.addMessage(
                        ChatMessage(
                            sessionId = sessionId,
                            role = MessageRole.ASSISTANT,
                            content = partial
                        )
                    )
                }
                _uiState.update {
                    it.copy(
                        streamState = StreamState.Error(e.message ?: "Unknown error"),
                        error = e.message,
                        currentStreamContent = ""
                    )
                }
            }
        }
    }

    private fun generateTitle(firstMessage: String) {
        val session = _uiState.value.session ?: return
        val endpoint = _uiState.value.endpoint ?: return

        viewModelScope.launch {
            val title = chatRepository.generateTitle(endpoint.url, session.modelName, firstMessage)
            if (title != null && title.isNotBlank()) {
                chatRepository.updateTitle(sessionId, title)
                _uiState.update { it.copy(session = it.session?.copy(title = title)) }
                titleGenerated = true
            }
        }
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val endpointRepository: EndpointRepository,
        private val preferences: AppPreferences,
        private val sessionId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(chatRepository, endpointRepository, preferences, sessionId) as T
        }
    }
}
