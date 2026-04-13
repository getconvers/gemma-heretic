package com.gemmaheretic.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gemmaheretic.app.data.local.preferences.AppPreferences
import com.gemmaheretic.app.data.repository.ChatRepository
import com.gemmaheretic.app.data.repository.EndpointRepository
import com.gemmaheretic.app.domain.model.ChatSession
import com.gemmaheretic.app.domain.model.Endpoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<ChatSession> = emptyList(),
    val endpoints: List<Endpoint> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val lastEndpointId: Long? = null,
    val lastModel: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val chatRepository: ChatRepository,
    private val endpointRepository: EndpointRepository,
    private val preferences: AppPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe endpoints
            endpointRepository.allEndpoints.collect { endpoints ->
                _uiState.update { it.copy(endpoints = endpoints) }
            }
        }

        viewModelScope.launch {
            preferences.lastEndpointId.collect { id ->
                _uiState.update { it.copy(lastEndpointId = id) }
            }
        }

        viewModelScope.launch {
            preferences.lastModel.collect { model ->
                _uiState.update { it.copy(lastModel = model) }
            }
        }

        viewModelScope.launch {
            _searchQuery.flatMapLatest { query ->
                if (query.isBlank()) chatRepository.allSessions
                else chatRepository.searchSessions(query)
            }.collect { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
        }
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateTitle(sessionId, newTitle)
        }
    }

    suspend fun createNewSession(endpointId: Long, endpointName: String, modelName: String): Long {
        val session = ChatSession(
            title = "New Chat",
            endpointId = endpointId,
            endpointName = endpointName,
            modelName = modelName
        )
        val id = chatRepository.createSession(session)
        preferences.setLastEndpointId(endpointId)
        preferences.setLastModel(modelName)
        return id
    }

    class Factory(
        private val chatRepository: ChatRepository,
        private val endpointRepository: EndpointRepository,
        private val preferences: AppPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(chatRepository, endpointRepository, preferences) as T
        }
    }
}
