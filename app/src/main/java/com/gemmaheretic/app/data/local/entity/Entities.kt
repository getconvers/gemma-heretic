package com.gemmaheretic.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "endpoints")
data class EndpointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val isDefault: Boolean = false,
    val defaultModel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val durationMs: Long? = null,
    val tokenCount: Int? = null
)

@Entity(tableName = "favorite_models")
data class FavoriteModelEntity(
    @PrimaryKey val name: String,
    val endpointId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
