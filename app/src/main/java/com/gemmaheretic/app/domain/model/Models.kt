package com.gemmaheretic.app.domain.model

data class Endpoint(
    val id: Long = 0,
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
    val defaultModel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: Long = 0,
    val title: String,
    val endpointId: Long,
    val endpointName: String = "",
    val modelName: String,
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val numCtx: Int? = null,
    val maxTokens: Int? = null,
    val repeatPenalty: Float? = null,
    val seed: Int? = null,
    val keepAlive: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

data class ChatMessage(
    val id: Long = 0,
    val sessionId: Long,
    val role: MessageRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long? = null,
    val tokenCount: Int? = null
)

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}

data class OllamaModel(
    val name: String,
    val size: Long = 0,
    val digest: String = "",
    val modifiedAt: String = "",
    val parameterSize: String = "",
    val quantizationLevel: String = ""
)

data class ChatConfig(
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val numCtx: Int? = null,
    val maxTokens: Int? = null,
    val repeatPenalty: Float? = null,
    val seed: Int? = null,
    val keepAlive: String? = null,
    val stream: Boolean = true
)

sealed class StreamState {
    data object Idle : StreamState()
    data object Streaming : StreamState()
    data class Error(val message: String) : StreamState()
    data object Complete : StreamState()
}

sealed class ConnectionStatus {
    data object Unknown : ConnectionStatus()
    data object Checking : ConnectionStatus()
    data object Connected : ConnectionStatus()
    data class Failed(val reason: String) : ConnectionStatus()
}
