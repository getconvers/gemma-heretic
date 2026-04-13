package com.gemmaheretic.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gemma_heretic_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_LAST_ENDPOINT_ID = longPreferencesKey("last_endpoint_id")
        private val KEY_LAST_MODEL = stringPreferencesKey("last_model")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode") // "system", "dark", "light"
        private val KEY_DEFAULT_SYSTEM_PROMPT = stringPreferencesKey("default_system_prompt")
        private val KEY_DEFAULT_TEMPERATURE = floatPreferencesKey("default_temperature")
        private val KEY_DEFAULT_TOP_P = floatPreferencesKey("default_top_p")
        private val KEY_DEFAULT_TOP_K = intPreferencesKey("default_top_k")
        private val KEY_DEFAULT_NUM_CTX = intPreferencesKey("default_num_ctx")
        private val KEY_DEFAULT_MAX_TOKENS = intPreferencesKey("default_max_tokens")
        private val KEY_DEFAULT_REPEAT_PENALTY = floatPreferencesKey("default_repeat_penalty")
        private val KEY_DEFAULT_SEED = intPreferencesKey("default_seed")
        private val KEY_DEFAULT_KEEP_ALIVE = stringPreferencesKey("default_keep_alive")
        private val KEY_STREAM_ENABLED = booleanPreferencesKey("stream_enabled")
        private val KEY_AUTO_TITLE = booleanPreferencesKey("auto_title")
        private val KEY_CONNECT_TIMEOUT = intPreferencesKey("connect_timeout_seconds")
        private val KEY_READ_TIMEOUT = intPreferencesKey("read_timeout_seconds")
    }

    val lastEndpointId: Flow<Long?> = context.dataStore.data.map { it[KEY_LAST_ENDPOINT_ID] }
    val lastModel: Flow<String?> = context.dataStore.data.map { it[KEY_LAST_MODEL] }
    val darkMode: Flow<String> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: "dark" }
    val defaultSystemPrompt: Flow<String?> = context.dataStore.data.map { it[KEY_DEFAULT_SYSTEM_PROMPT] }
    val defaultTemperature: Flow<Float?> = context.dataStore.data.map { it[KEY_DEFAULT_TEMPERATURE] }
    val defaultTopP: Flow<Float?> = context.dataStore.data.map { it[KEY_DEFAULT_TOP_P] }
    val defaultTopK: Flow<Int?> = context.dataStore.data.map { it[KEY_DEFAULT_TOP_K] }
    val defaultNumCtx: Flow<Int?> = context.dataStore.data.map { it[KEY_DEFAULT_NUM_CTX] }
    val defaultMaxTokens: Flow<Int?> = context.dataStore.data.map { it[KEY_DEFAULT_MAX_TOKENS] }
    val defaultRepeatPenalty: Flow<Float?> = context.dataStore.data.map { it[KEY_DEFAULT_REPEAT_PENALTY] }
    val defaultSeed: Flow<Int?> = context.dataStore.data.map { it[KEY_DEFAULT_SEED] }
    val defaultKeepAlive: Flow<String?> = context.dataStore.data.map { it[KEY_DEFAULT_KEEP_ALIVE] }
    val streamEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_STREAM_ENABLED] ?: true }
    val autoTitle: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_TITLE] ?: true }
    val connectTimeout: Flow<Int> = context.dataStore.data.map { it[KEY_CONNECT_TIMEOUT] ?: 10 }
    val readTimeout: Flow<Int> = context.dataStore.data.map { it[KEY_READ_TIMEOUT] ?: 120 }

    suspend fun setLastEndpointId(id: Long) {
        context.dataStore.edit { it[KEY_LAST_ENDPOINT_ID] = id }
    }

    suspend fun setLastModel(model: String) {
        context.dataStore.edit { it[KEY_LAST_MODEL] = model }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[KEY_DARK_MODE] = mode }
    }

    suspend fun setDefaultSystemPrompt(prompt: String?) {
        context.dataStore.edit {
            if (prompt == null) it.remove(KEY_DEFAULT_SYSTEM_PROMPT)
            else it[KEY_DEFAULT_SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun setDefaultTemperature(value: Float?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_TEMPERATURE)
            else it[KEY_DEFAULT_TEMPERATURE] = value
        }
    }

    suspend fun setDefaultTopP(value: Float?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_TOP_P)
            else it[KEY_DEFAULT_TOP_P] = value
        }
    }

    suspend fun setDefaultTopK(value: Int?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_TOP_K)
            else it[KEY_DEFAULT_TOP_K] = value
        }
    }

    suspend fun setDefaultNumCtx(value: Int?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_NUM_CTX)
            else it[KEY_DEFAULT_NUM_CTX] = value
        }
    }

    suspend fun setDefaultMaxTokens(value: Int?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_MAX_TOKENS)
            else it[KEY_DEFAULT_MAX_TOKENS] = value
        }
    }

    suspend fun setDefaultRepeatPenalty(value: Float?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_REPEAT_PENALTY)
            else it[KEY_DEFAULT_REPEAT_PENALTY] = value
        }
    }

    suspend fun setDefaultSeed(value: Int?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_SEED)
            else it[KEY_DEFAULT_SEED] = value
        }
    }

    suspend fun setDefaultKeepAlive(value: String?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_DEFAULT_KEEP_ALIVE)
            else it[KEY_DEFAULT_KEEP_ALIVE] = value
        }
    }

    suspend fun setStreamEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_STREAM_ENABLED] = enabled }
    }

    suspend fun setAutoTitle(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_TITLE] = enabled }
    }

    suspend fun setConnectTimeout(seconds: Int) {
        context.dataStore.edit { it[KEY_CONNECT_TIMEOUT] = seconds }
    }

    suspend fun setReadTimeout(seconds: Int) {
        context.dataStore.edit { it[KEY_READ_TIMEOUT] = seconds }
    }
}
