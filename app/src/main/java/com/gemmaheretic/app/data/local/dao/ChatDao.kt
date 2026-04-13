package com.gemmaheretic.app.data.local.dao

import androidx.room.*
import com.gemmaheretic.app.data.local.entity.ChatMessageEntity
import com.gemmaheretic.app.data.local.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

data class ChatSessionWithCount(
    val id: Long,
    val title: String,
    val endpointId: Long,
    val endpointName: String,
    val modelName: String,
    val systemPrompt: String?,
    val temperature: Float?,
    val topP: Float?,
    val topK: Int?,
    val numCtx: Int?,
    val maxTokens: Int?,
    val repeatPenalty: Float?,
    val seed: Int?,
    val keepAlive: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int
)

@Dao
interface ChatSessionDao {
    @Query("""
        SELECT s.*, COUNT(m.id) as messageCount
        FROM chat_sessions s
        LEFT JOIN chat_messages m ON m.sessionId = s.id
        GROUP BY s.id
        ORDER BY s.updatedAt DESC
    """)
    fun getAllSessionsWithCount(): Flow<List<ChatSessionWithCount>>

    @Query("""
        SELECT s.*, COUNT(m.id) as messageCount
        FROM chat_sessions s
        LEFT JOIN chat_messages m ON m.sessionId = s.id
        WHERE s.title LIKE '%' || :query || '%'
        GROUP BY s.id
        ORDER BY s.updatedAt DESC
    """)
    fun searchSessions(query: String): Flow<List<ChatSessionWithCount>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity): Long

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("UPDATE chat_sessions SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touchSession(id: Long, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND createdAt >= :afterTimestamp")
    suspend fun deleteMessagesAfter(sessionId: Long, afterTimestamp: Long)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteAllMessages(sessionId: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: Long): Int
}
