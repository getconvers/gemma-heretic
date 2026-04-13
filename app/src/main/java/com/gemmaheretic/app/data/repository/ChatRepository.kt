package com.gemmaheretic.app.data.repository

import com.gemmaheretic.app.data.local.dao.ChatMessageDao
import com.gemmaheretic.app.data.local.dao.ChatSessionDao
import com.gemmaheretic.app.data.local.entity.ChatMessageEntity
import com.gemmaheretic.app.data.local.entity.ChatSessionEntity
import com.gemmaheretic.app.domain.model.ChatMessage
import com.gemmaheretic.app.domain.model.ChatSession
import com.gemmaheretic.app.domain.model.MessageRole
import com.gemmaheretic.app.network.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepository(
    private val chatSessionDao: ChatSessionDao,
    private val chatMessageDao: ChatMessageDao
) {
    val allSessions: Flow<List<ChatSession>> = chatSessionDao.getAllSessionsWithCount().map { list ->
        list.map { it.toDomain() }
    }

    fun searchSessions(query: String): Flow<List<ChatSession>> {
        return chatSessionDao.searchSessions(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatMessageDao.getMessagesForSession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getMessagesOnce(sessionId: Long): List<ChatMessage> {
        return chatMessageDao.getMessagesForSessionOnce(sessionId).map { it.toDomain() }
    }

    suspend fun getSession(id: Long): ChatSession? {
        return chatSessionDao.getSessionById(id)?.let { entity ->
            val count = chatMessageDao.getMessageCount(id)
            ChatSession(
                id = entity.id,
                title = entity.title,
                endpointId = entity.endpointId,
                endpointName = entity.endpointName,
                modelName = entity.modelName,
                systemPrompt = entity.systemPrompt,
                temperature = entity.temperature,
                topP = entity.topP,
                topK = entity.topK,
                numCtx = entity.numCtx,
                maxTokens = entity.maxTokens,
                repeatPenalty = entity.repeatPenalty,
                seed = entity.seed,
                keepAlive = entity.keepAlive,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                messageCount = count
            )
        }
    }

    suspend fun createSession(session: ChatSession): Long {
        return chatSessionDao.insertSession(session.toEntity())
    }

    suspend fun updateSession(session: ChatSession) {
        chatSessionDao.updateSession(session.toEntity())
    }

    suspend fun updateTitle(sessionId: Long, title: String) {
        chatSessionDao.updateTitle(sessionId, title)
    }

    suspend fun deleteSession(sessionId: Long) {
        chatSessionDao.deleteSession(sessionId)
    }

    suspend fun touchSession(sessionId: Long) {
        chatSessionDao.touchSession(sessionId)
    }

    suspend fun addMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(message.toEntity())
    }

    suspend fun updateMessage(message: ChatMessage) {
        chatMessageDao.updateMessage(message.toEntity())
    }

    suspend fun deleteMessage(messageId: Long) {
        chatMessageDao.deleteMessage(messageId)
    }

    suspend fun deleteMessagesAfter(sessionId: Long, afterTimestamp: Long) {
        chatMessageDao.deleteMessagesAfter(sessionId, afterTimestamp)
    }

    suspend fun clearChat(sessionId: Long) {
        chatMessageDao.deleteAllMessages(sessionId)
    }

    suspend fun chatNonStreaming(
        baseUrl: String,
        request: OllamaChatRequest
    ): Result<OllamaChatResponse> {
        return try {
            val api = EndpointRepository.createApiService(baseUrl)
            val response = api.chatNonStreaming(request)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTitle(baseUrl: String, modelName: String, firstMessage: String): String? {
        return try {
            val request = OllamaChatRequest(
                model = modelName,
                messages = listOf(
                    OllamaChatMessage(
                        role = "user",
                        content = "Generate a very brief title (3-6 words, no quotes) for a conversation that starts with this message:\n\n$firstMessage"
                    )
                ),
                stream = false,
                options = OllamaOptions(temperature = 0.3f, numPredict = 20)
            )
            val result = chatNonStreaming(baseUrl, request)
            result.getOrNull()?.message?.content?.trim()?.take(100)
        } catch (e: Exception) {
            null
        }
    }

    // --- Mappers ---

    private fun com.gemmaheretic.app.data.local.dao.ChatSessionWithCount.toDomain() = ChatSession(
        id = id,
        title = title,
        endpointId = endpointId,
        endpointName = endpointName,
        modelName = modelName,
        systemPrompt = systemPrompt,
        temperature = temperature,
        topP = topP,
        topK = topK,
        numCtx = numCtx,
        maxTokens = maxTokens,
        repeatPenalty = repeatPenalty,
        seed = seed,
        keepAlive = keepAlive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        messageCount = messageCount
    )

    private fun ChatMessageEntity.toDomain() = ChatMessage(
        id = id,
        sessionId = sessionId,
        role = MessageRole.entries.first { it.value == role },
        content = content,
        createdAt = createdAt,
        durationMs = durationMs,
        tokenCount = tokenCount
    )

    private fun ChatMessage.toEntity() = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = role.value,
        content = content,
        createdAt = createdAt,
        durationMs = durationMs,
        tokenCount = tokenCount
    )

    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        title = title,
        endpointId = endpointId,
        endpointName = endpointName,
        modelName = modelName,
        systemPrompt = systemPrompt,
        temperature = temperature,
        topP = topP,
        topK = topK,
        numCtx = numCtx,
        maxTokens = maxTokens,
        repeatPenalty = repeatPenalty,
        seed = seed,
        keepAlive = keepAlive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
