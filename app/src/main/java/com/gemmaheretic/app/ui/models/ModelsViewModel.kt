package com.gemmaheretic.app.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gemmaheretic.app.data.local.dao.FavoriteModelDao
import com.gemmaheretic.app.data.local.entity.FavoriteModelEntity
import com.gemmaheretic.app.data.repository.EndpointRepository
import com.gemmaheretic.app.domain.model.Endpoint
import com.gemmaheretic.app.domain.model.OllamaModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ModelsUiState(
    val models: List<OllamaModel> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val endpoints: List<Endpoint> = emptyList(),
    val selectedEndpoint: Endpoint? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ModelsViewModel(
    private val endpointRepository: EndpointRepository,
    private val favoriteModelDao: FavoriteModelDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelsUiState())
    val uiState: StateFlow<ModelsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            endpointRepository.allEndpoints.collect { endpoints ->
                _uiState.update { state ->
                    val selected = state.selectedEndpoint
                        ?: endpoints.find { it.isDefault }
                        ?: endpoints.firstOrNull()
                    state.copy(endpoints = endpoints, selectedEndpoint = selected)
                }
                // Auto-load models for default endpoint
                val endpoint = _uiState.value.selectedEndpoint
                if (endpoint != null && _uiState.value.models.isEmpty()) {
                    refreshModels()
                }
            }
        }

        viewModelScope.launch {
            favoriteModelDao.getAllFavorites().collect { favs ->
                _uiState.update { it.copy(favorites = favs.map { f -> f.name }.toSet()) }
            }
        }
    }

    fun selectEndpoint(endpoint: Endpoint) {
        _uiState.update { it.copy(selectedEndpoint = endpoint) }
        refreshModels()
    }

    fun refreshModels() {
        val endpoint = _uiState.value.selectedEndpoint ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = endpointRepository.fetchModels(endpoint.url)
            result.onSuccess { models ->
                _uiState.update { it.copy(models = models, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleFavorite(modelName: String) {
        viewModelScope.launch {
            val endpointId = _uiState.value.selectedEndpoint?.id ?: return@launch
            if (favoriteModelDao.isFavorite(modelName)) {
                favoriteModelDao.removeFavorite(modelName)
            } else {
                favoriteModelDao.addFavorite(FavoriteModelEntity(name = modelName, endpointId = endpointId))
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    class Factory(
        private val endpointRepository: EndpointRepository,
        private val favoriteModelDao: FavoriteModelDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ModelsViewModel(endpointRepository, favoriteModelDao) as T
        }
    }
}
