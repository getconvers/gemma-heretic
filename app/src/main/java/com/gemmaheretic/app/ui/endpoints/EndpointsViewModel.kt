package com.gemmaheretic.app.ui.endpoints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gemmaheretic.app.data.repository.EndpointRepository
import com.gemmaheretic.app.domain.model.ConnectionStatus
import com.gemmaheretic.app.domain.model.Endpoint
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EndpointsUiState(
    val endpoints: List<Endpoint> = emptyList(),
    val connectionStatuses: Map<Long, ConnectionStatus> = emptyMap(),
    val showAddDialog: Boolean = false,
    val editingEndpoint: Endpoint? = null,
    val isLoading: Boolean = true
)

class EndpointsViewModel(
    private val endpointRepository: EndpointRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EndpointsUiState())
    val uiState: StateFlow<EndpointsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            endpointRepository.allEndpoints.collect { endpoints ->
                _uiState.update { it.copy(endpoints = endpoints, isLoading = false) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingEndpoint = null) }
    }

    fun showEditDialog(endpoint: Endpoint) {
        _uiState.update { it.copy(showAddDialog = true, editingEndpoint = endpoint) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingEndpoint = null) }
    }

    fun saveEndpoint(name: String, url: String, isDefault: Boolean, defaultModel: String?) {
        viewModelScope.launch {
            val editing = _uiState.value.editingEndpoint
            if (editing != null) {
                endpointRepository.updateEndpoint(
                    editing.copy(name = name, url = url, isDefault = isDefault, defaultModel = defaultModel)
                )
                if (isDefault) endpointRepository.setDefault(editing.id)
            } else {
                val id = endpointRepository.saveEndpoint(
                    Endpoint(name = name, url = url, isDefault = isDefault, defaultModel = defaultModel)
                )
                if (isDefault) endpointRepository.setDefault(id)
            }
            dismissDialog()
        }
    }

    fun deleteEndpoint(endpoint: Endpoint) {
        viewModelScope.launch {
            endpointRepository.deleteEndpoint(endpoint)
        }
    }

    fun setDefault(id: Long) {
        viewModelScope.launch {
            endpointRepository.setDefault(id)
        }
    }

    fun testConnection(endpoint: Endpoint) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(connectionStatuses = it.connectionStatuses + (endpoint.id to ConnectionStatus.Checking))
            }
            val status = endpointRepository.testConnection(endpoint.url)
            _uiState.update {
                it.copy(connectionStatuses = it.connectionStatuses + (endpoint.id to status))
            }
        }
    }

    class Factory(
        private val endpointRepository: EndpointRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EndpointsViewModel(endpointRepository) as T
        }
    }
}
