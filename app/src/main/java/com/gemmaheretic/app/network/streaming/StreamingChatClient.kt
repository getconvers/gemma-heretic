package com.gemmaheretic.app.network.streaming

import com.gemmaheretic.app.network.api.OllamaChatRequest
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lightweight streaming client that reads tokens directly via Okio.
 * No callbackFlow, no Gson per-token, no BufferedReader wrapping.
 */
class StreamingChatClient {

    companion object {
        // Shared client — reuses connection pool and threads across all requests
        private var sharedClient: OkHttpClient? = null

        fun getClient(connectTimeout: Int = 10, readTimeout: Int = 120): OkHttpClient {
            return sharedClient ?: OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toLong(), TimeUnit.SECONDS)
                .readTimeout(readTimeout.toLong(), TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
                .also { sharedClient = it }
        }
    }

    // Default Gson omits null fields — Ollama uses its own defaults
    private val gson = Gson()

    data class StreamResult(
        val totalDuration: Long? = null,
        val evalCount: Int? = null,
        val evalDuration: Long? = null,
        val promptEvalCount: Int? = null,
        val promptEvalDuration: Long? = null,
        val loadDuration: Long? = null
    ) {
        /** Server-side generation speed: eval_count / eval_duration (nanoseconds) */
        val tokensPerSecond: Double?
            get() {
                val count = evalCount ?: return null
                val dur = evalDuration ?: return null
                if (dur <= 0) return null
                return count.toDouble() / dur * 1_000_000_000.0
            }
    }

    /**
     * Streams chat directly, calling onToken for each token and returning
     * metadata on completion. Runs blocking on the caller's thread — call
     * from Dispatchers.IO.
     */
    fun streamBlocking(
        baseUrl: String,
        request: OllamaChatRequest,
        onToken: (String) -> Unit,
        connectTimeout: Int = 10,
        readTimeout: Int = 120
    ): StreamResult {
        val client = getClient(connectTimeout, readTimeout)
        val url = baseUrl.trimEnd('/') + "/api/chat"
        val json = gson.toJson(request)
        val body = json.toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(httpRequest)
        val response = call.execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            response.close()
            throw IOException("HTTP ${response.code}: $errorBody")
        }

        val responseBody = response.body ?: throw IOException("Empty response body")
        var evalCount: Int? = null
        var totalDuration: Long? = null
        var evalDuration: Long? = null
        var promptEvalCount: Int? = null
        var promptEvalDuration: Long? = null
        var loadDuration: Long? = null

        try {
            // Use Okio BufferedSource directly — no InputStream/Reader wrapping
            val source = responseBody.source()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue

                // Check for done FIRST (avoids unnecessary content extraction)
                if (line.contains("\"done\":true")) {
                    try {
                        evalCount = extractInt(line, "\"eval_count\":")
                        totalDuration = extractLong(line, "\"total_duration\":")
                        evalDuration = extractLong(line, "\"eval_duration\":")
                        promptEvalCount = extractInt(line, "\"prompt_eval_count\":")
                        promptEvalDuration = extractLong(line, "\"prompt_eval_duration\":")
                        loadDuration = extractLong(line, "\"load_duration\":")
                    } catch (_: Exception) {}
                    break
                }

                // Fast manual extraction of content field — no Gson reflection
                val token = extractContent(line)
                if (token != null && token.isNotEmpty()) {
                    onToken(token)
                }
            }
        } finally {
            responseBody.close()
        }

        return StreamResult(
            totalDuration = totalDuration,
            evalCount = evalCount,
            evalDuration = evalDuration,
            promptEvalCount = promptEvalCount,
            promptEvalDuration = promptEvalDuration,
            loadDuration = loadDuration
        )
    }

    fun cancelAll() {
        sharedClient?.dispatcher?.cancelAll()
    }
}

// --- Fast JSON field extraction (no reflection, no object allocation) ---

internal fun extractContent(json: String): String? {
    // Find "content":" and extract the string value with escape handling
    val key = "\"content\":\""
    val start = json.indexOf(key)
    if (start == -1) return null
    val contentStart = start + key.length
    val sb = StringBuilder()
    var i = contentStart
    while (i < json.length) {
        val c = json[i]
        if (c == '\\' && i + 1 < json.length) {
            when (json[i + 1]) {
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                'n' -> { sb.append('\n'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                '/' -> { sb.append('/'); i += 2 }
                'u' -> {
                    // Unicode escape \uXXXX
                    if (i + 5 < json.length) {
                        try {
                            val hex = json.substring(i + 2, i + 6)
                            sb.append(hex.toInt(16).toChar())
                            i += 6
                        } catch (_: Exception) { sb.append(c); i++ }
                    } else { sb.append(c); i++ }
                }
                else -> { sb.append(c); i++ }
            }
        } else if (c == '"') {
            break
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

internal fun extractInt(json: String, key: String): Int? {
    val start = json.indexOf(key)
    if (start == -1) return null
    val numStart = start + key.length
    val numEnd = json.indexOfAny(charArrayOf(',', '}', ' '), numStart)
    if (numEnd == -1) return null
    return json.substring(numStart, numEnd).trim().toIntOrNull()
}

internal fun extractLong(json: String, key: String): Long? {
    val start = json.indexOf(key)
    if (start == -1) return null
    val numStart = start + key.length
    val numEnd = json.indexOfAny(charArrayOf(',', '}', ' '), numStart)
    if (numEnd == -1) return null
    return json.substring(numStart, numEnd).trim().toLongOrNull()
}
