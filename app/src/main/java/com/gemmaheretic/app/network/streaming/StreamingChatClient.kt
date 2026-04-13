package com.gemmaheretic.app.network.streaming

import com.gemmaheretic.app.network.api.OllamaChatRequest
import com.gemmaheretic.app.network.api.OllamaChatResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class StreamingChatClient(
    private val connectTimeoutSeconds: Int = 10,
    private val readTimeoutSeconds: Int = 120
) {
    private val gson = Gson()

    fun streamChat(
        baseUrl: String,
        request: OllamaChatRequest
    ): Flow<StreamEvent> = callbackFlow {
        val client = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val url = baseUrl.trimEnd('/') + "/api/chat"
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()

        var call: Call? = null

        try {
            call = client.newCall(httpRequest)

            val response = withContext(Dispatchers.IO) {
                call.execute()
            }

            if (!response.isSuccessful) {
                val errorBody = withContext(Dispatchers.IO) {
                    response.body?.string() ?: "Unknown error"
                }
                trySend(StreamEvent.Error("HTTP ${response.code}: $errorBody"))
                close()
                return@callbackFlow
            }

            val responseBody = response.body
            if (responseBody == null) {
                trySend(StreamEvent.Error("Empty response body"))
                close()
                return@callbackFlow
            }

            withContext(Dispatchers.IO) {
                val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        if (line.isNullOrBlank()) continue
                        try {
                            val chatResponse = gson.fromJson(line, OllamaChatResponse::class.java)
                            if (chatResponse.done) {
                                trySend(StreamEvent.Done(chatResponse))
                            } else {
                                val token = chatResponse.message?.content ?: ""
                                if (token.isNotEmpty()) {
                                    trySend(StreamEvent.Token(token))
                                }
                            }
                        } catch (e: Exception) {
                            // Skip malformed lines
                        }
                    }
                } catch (e: IOException) {
                    if (call?.isCanceled() != true) {
                        trySend(StreamEvent.Error("Stream interrupted: ${e.message}"))
                    }
                } finally {
                    reader.close()
                    responseBody.close()
                }
            }

            close()
        } catch (e: IOException) {
            if (call?.isCanceled() != true) {
                trySend(StreamEvent.Error("Connection failed: ${e.message}"))
            }
            close()
        } catch (e: Exception) {
            trySend(StreamEvent.Error("Unexpected error: ${e.message}"))
            close()
        }

        awaitClose {
            call?.cancel()
        }
    }
}

sealed class StreamEvent {
    data class Token(val text: String) : StreamEvent()
    data class Done(val response: OllamaChatResponse) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
