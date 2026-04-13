package com.gemmaheretic.app.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

// --- Request Models ---

data class OllamaChatRequest(
    val model: String,
    val messages: List<OllamaChatMessage>,
    val stream: Boolean = true,
    val options: OllamaOptions? = null,
    @SerializedName("keep_alive") val keepAlive: String? = null
)

data class OllamaChatMessage(
    val role: String,
    val content: String
)

data class OllamaOptions(
    val temperature: Float? = null,
    @SerializedName("top_p") val topP: Float? = null,
    @SerializedName("top_k") val topK: Int? = null,
    @SerializedName("num_ctx") val numCtx: Int? = null,
    @SerializedName("num_predict") val numPredict: Int? = null,
    @SerializedName("repeat_penalty") val repeatPenalty: Float? = null,
    val seed: Int? = null
)

// --- Response Models ---

data class OllamaChatResponse(
    val model: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val message: OllamaChatMessage? = null,
    val done: Boolean = false,
    @SerializedName("total_duration") val totalDuration: Long? = null,
    @SerializedName("eval_count") val evalCount: Int? = null,
    @SerializedName("eval_duration") val evalDuration: Long? = null,
    @SerializedName("prompt_eval_count") val promptEvalCount: Int? = null
)

data class OllamaModelListResponse(
    val models: List<OllamaModelInfo>? = null
)

data class OllamaModelInfo(
    val name: String,
    val size: Long = 0,
    val digest: String = "",
    @SerializedName("modified_at") val modifiedAt: String = "",
    val details: OllamaModelDetails? = null
)

data class OllamaModelDetails(
    @SerializedName("parameter_size") val parameterSize: String = "",
    @SerializedName("quantization_level") val quantizationLevel: String = "",
    val family: String = "",
    val format: String = ""
)

data class OllamaVersionResponse(
    val version: String = ""
)

// --- Retrofit Interface (non-streaming calls only) ---

interface OllamaApiService {
    @GET("/api/tags")
    suspend fun listModels(): Response<OllamaModelListResponse>

    @GET("/api/version")
    suspend fun getVersion(): Response<OllamaVersionResponse>

    @POST("/api/chat")
    suspend fun chatNonStreaming(@Body request: OllamaChatRequest): Response<OllamaChatResponse>
}
