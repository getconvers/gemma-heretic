package com.gemmaheretic.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gemmaheretic.app.data.repository.EndpointRepository
import com.gemmaheretic.app.domain.model.Endpoint
import com.gemmaheretic.app.domain.model.OllamaModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatSheet(
    endpoints: List<Endpoint>,
    lastEndpointId: Long?,
    lastModel: String?,
    endpointRepository: EndpointRepository,
    onCreateChat: suspend (endpointId: Long, endpointName: String, modelName: String) -> Long,
    onChatCreated: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // Select initial endpoint
    val initialEndpoint = endpoints.find { it.id == lastEndpointId }
        ?: endpoints.find { it.isDefault }
        ?: endpoints.firstOrNull()

    var selectedEndpoint by remember { mutableStateOf(initialEndpoint) }
    var models by remember { mutableStateOf<List<OllamaModel>>(emptyList()) }
    var isLoadingModels by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load models when endpoint changes
    LaunchedEffect(selectedEndpoint) {
        selectedEndpoint?.let { ep ->
            isLoadingModels = true
            error = null
            val result = endpointRepository.fetchModels(ep.url)
            result.onSuccess { models = it }
                .onFailure { error = it.message }
            isLoadingModels = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "New Chat",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Endpoint selector
            if (endpoints.size > 1) {
                Text(
                    "Endpoint",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    endpoints.forEach { ep ->
                        FilterChip(
                            selected = selectedEndpoint?.id == ep.id,
                            onClick = { selectedEndpoint = ep },
                            label = { Text(ep.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        )
                    }
                }
            }

            // Model list
            Text(
                "Select Model",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            error?.let { msg ->
                Text(
                    text = "Error: $msg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isLoadingModels) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (models.isEmpty()) {
                Text(
                    "No models found. Make sure Ollama is running and has models pulled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(models) { model ->
                        val isLastUsed = model.name == lastModel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val ep = selectedEndpoint ?: return@launch
                                        val id = onCreateChat(ep.id, ep.name, model.name)
                                        onChatCreated(id)
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLastUsed) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (model.parameterSize.isNotBlank()) {
                                            Text(
                                                text = model.parameterSize,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (model.quantizationLevel.isNotBlank()) {
                                            Text(
                                                text = model.quantizationLevel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        val sizeMB = model.size / (1024 * 1024)
                                        if (sizeMB > 0) {
                                            Text(
                                                text = if (sizeMB > 1024) "${sizeMB / 1024}GB" else "${sizeMB}MB",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                if (isLastUsed) {
                                    Text(
                                        "Recent",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
