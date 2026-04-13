package com.gemmaheretic.app.ui.endpoints

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gemmaheretic.app.domain.model.ConnectionStatus
import com.gemmaheretic.app.domain.model.Endpoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EndpointsScreen(
    viewModel: EndpointsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Endpoints") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add endpoint")
            }
        }
    ) { padding ->
        if (uiState.endpoints.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Dns,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No endpoints configured",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Add an Ollama server endpoint to connect to.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Common endpoints:\n• http://localhost:11434 (Termux on same device)\n• http://192.168.x.x:11434 (LAN server)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.endpoints, key = { it.id }) { endpoint ->
                    EndpointCard(
                        endpoint = endpoint,
                        connectionStatus = uiState.connectionStatuses[endpoint.id],
                        onEdit = { viewModel.showEditDialog(endpoint) },
                        onDelete = { viewModel.deleteEndpoint(endpoint) },
                        onTest = { viewModel.testConnection(endpoint) },
                        onSetDefault = { viewModel.setDefault(endpoint.id) }
                    )
                }
            }
        }

        if (uiState.showAddDialog) {
            EndpointDialog(
                endpoint = uiState.editingEndpoint,
                onDismiss = viewModel::dismissDialog,
                onSave = viewModel::saveEndpoint
            )
        }
    }
}

@Composable
private fun EndpointCard(
    endpoint: Endpoint,
    connectionStatus: ConnectionStatus?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit,
    onSetDefault: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = endpoint.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (endpoint.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    Text(
                        text = endpoint.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    endpoint.defaultModel?.let { model ->
                        Text(
                            text = "Default: $model",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Connection status
                when (connectionStatus) {
                    is ConnectionStatus.Checking -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                    is ConnectionStatus.Connected -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    is ConnectionStatus.Failed -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    else -> {}
                }
            }

            if (connectionStatus is ConnectionStatus.Failed) {
                Text(
                    text = connectionStatus.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onTest) { Text("Test") }
                if (!endpoint.isDefault) {
                    TextButton(onClick = onSetDefault) { Text("Set Default") }
                }
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EndpointDialog(
    endpoint: Endpoint?,
    onDismiss: () -> Unit,
    onSave: (name: String, url: String, isDefault: Boolean, defaultModel: String?) -> Unit
) {
    var name by remember { mutableStateOf(endpoint?.name ?: "") }
    var url by remember { mutableStateOf(endpoint?.url ?: "http://localhost:11434") }
    var isDefault by remember { mutableStateOf(endpoint?.isDefault ?: false) }
    var defaultModel by remember { mutableStateOf(endpoint?.defaultModel ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (endpoint != null) "Edit Endpoint" else "Add Endpoint") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    placeholder = { Text("My Server") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    placeholder = { Text("http://localhost:11434") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = defaultModel,
                    onValueChange = { defaultModel = it },
                    label = { Text("Default Model (optional)") },
                    singleLine = true,
                    placeholder = { Text("llama3.2") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("Set as default endpoint", style = MaterialTheme.typography.bodyMedium)
                }

                // Help text
                Text(
                    text = "For Termux on the same device, use http://localhost:11434 or http://127.0.0.1:11434. For LAN servers, use the server's IP address.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && url.isNotBlank()) {
                        onSave(name, url.trimEnd('/'), isDefault, defaultModel.ifBlank { null })
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
