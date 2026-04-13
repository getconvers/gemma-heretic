package com.gemmaheretic.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gemmaheretic.app.data.local.preferences.AppPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val darkMode: String = "dark",
    val defaultSystemPrompt: String = "",
    val defaultTemperature: String = "",
    val defaultTopP: String = "",
    val defaultTopK: String = "",
    val defaultNumCtx: String = "",
    val defaultMaxTokens: String = "",
    val defaultRepeatPenalty: String = "",
    val defaultSeed: String = "",
    val defaultKeepAlive: String = "",
    val streamEnabled: Boolean = true,
    val autoTitle: Boolean = true,
    val connectTimeout: String = "10",
    val readTimeout: String = "120"
)

class SettingsViewModel(
    private val preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.darkMode,
                preferences.defaultSystemPrompt,
                preferences.defaultTemperature,
                preferences.defaultTopP,
                preferences.defaultTopK
            ) { darkMode, systemPrompt, temp, topP, topK ->
                _uiState.update {
                    it.copy(
                        darkMode = darkMode,
                        defaultSystemPrompt = systemPrompt ?: "",
                        defaultTemperature = temp?.toString() ?: "",
                        defaultTopP = topP?.toString() ?: "",
                        defaultTopK = topK?.toString() ?: ""
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            combine(
                preferences.defaultNumCtx,
                preferences.defaultMaxTokens,
                preferences.defaultRepeatPenalty,
                preferences.defaultSeed,
                preferences.defaultKeepAlive
            ) { numCtx, maxTokens, repeatPenalty, seed, keepAlive ->
                _uiState.update {
                    it.copy(
                        defaultNumCtx = numCtx?.toString() ?: "",
                        defaultMaxTokens = maxTokens?.toString() ?: "",
                        defaultRepeatPenalty = repeatPenalty?.toString() ?: "",
                        defaultSeed = seed?.toString() ?: "",
                        defaultKeepAlive = keepAlive ?: ""
                    )
                }
            }.collect()
        }

        viewModelScope.launch {
            combine(
                preferences.streamEnabled,
                preferences.autoTitle,
                preferences.connectTimeout,
                preferences.readTimeout
            ) { stream, autoTitle, connectTimeout, readTimeout ->
                _uiState.update {
                    it.copy(
                        streamEnabled = stream,
                        autoTitle = autoTitle,
                        connectTimeout = connectTimeout.toString(),
                        readTimeout = readTimeout.toString()
                    )
                }
            }.collect()
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { preferences.setDarkMode(mode) }
    }

    fun setDefaultSystemPrompt(prompt: String) {
        viewModelScope.launch { preferences.setDefaultSystemPrompt(prompt.ifBlank { null }) }
    }

    fun setDefaultTemperature(value: String) {
        viewModelScope.launch { preferences.setDefaultTemperature(value.toFloatOrNull()) }
    }

    fun setDefaultTopP(value: String) {
        viewModelScope.launch { preferences.setDefaultTopP(value.toFloatOrNull()) }
    }

    fun setDefaultTopK(value: String) {
        viewModelScope.launch { preferences.setDefaultTopK(value.toIntOrNull()) }
    }

    fun setDefaultNumCtx(value: String) {
        viewModelScope.launch { preferences.setDefaultNumCtx(value.toIntOrNull()) }
    }

    fun setDefaultMaxTokens(value: String) {
        viewModelScope.launch { preferences.setDefaultMaxTokens(value.toIntOrNull()) }
    }

    fun setDefaultRepeatPenalty(value: String) {
        viewModelScope.launch { preferences.setDefaultRepeatPenalty(value.toFloatOrNull()) }
    }

    fun setDefaultSeed(value: String) {
        viewModelScope.launch { preferences.setDefaultSeed(value.toIntOrNull()) }
    }

    fun setDefaultKeepAlive(value: String) {
        viewModelScope.launch { preferences.setDefaultKeepAlive(value.ifBlank { null }) }
    }

    fun setStreamEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setStreamEnabled(enabled) }
    }

    fun setAutoTitle(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoTitle(enabled) }
    }

    fun setConnectTimeout(value: String) {
        viewModelScope.launch { value.toIntOrNull()?.let { preferences.setConnectTimeout(it) } }
    }

    fun setReadTimeout(value: String) {
        viewModelScope.launch { value.toIntOrNull()?.let { preferences.setReadTimeout(it) } }
    }

    class Factory(
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferences) as T
        }
    }
}
