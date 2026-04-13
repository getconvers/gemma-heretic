package com.gemmaheretic.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onDarkModeChange: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance
            SectionHeader("Appearance")
            ThemeSelector(
                selected = uiState.darkMode,
                onSelect = {
                    viewModel.setDarkMode(it)
                    onDarkModeChange(it)
                }
            )

            // Chat Defaults
            SectionHeader("Chat Defaults")
            SettingsTextField(
                label = "Default System Prompt",
                value = uiState.defaultSystemPrompt,
                onValueChange = viewModel::setDefaultSystemPrompt,
                maxLines = 4
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTextField(
                    label = "Temperature",
                    value = uiState.defaultTemperature,
                    onValueChange = viewModel::setDefaultTemperature,
                    modifier = Modifier.weight(1f),
                    placeholder = "0.7"
                )
                SettingsTextField(
                    label = "Top P",
                    value = uiState.defaultTopP,
                    onValueChange = viewModel::setDefaultTopP,
                    modifier = Modifier.weight(1f),
                    placeholder = "0.9"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTextField(
                    label = "Top K",
                    value = uiState.defaultTopK,
                    onValueChange = viewModel::setDefaultTopK,
                    modifier = Modifier.weight(1f),
                    placeholder = "40"
                )
                SettingsTextField(
                    label = "Context Size",
                    value = uiState.defaultNumCtx,
                    onValueChange = viewModel::setDefaultNumCtx,
                    modifier = Modifier.weight(1f),
                    placeholder = "2048"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTextField(
                    label = "Max Tokens",
                    value = uiState.defaultMaxTokens,
                    onValueChange = viewModel::setDefaultMaxTokens,
                    modifier = Modifier.weight(1f),
                    placeholder = "512"
                )
                SettingsTextField(
                    label = "Repeat Penalty",
                    value = uiState.defaultRepeatPenalty,
                    onValueChange = viewModel::setDefaultRepeatPenalty,
                    modifier = Modifier.weight(1f),
                    placeholder = "1.1"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTextField(
                    label = "Seed",
                    value = uiState.defaultSeed,
                    onValueChange = viewModel::setDefaultSeed,
                    modifier = Modifier.weight(1f),
                    placeholder = "-1"
                )
                SettingsTextField(
                    label = "Keep Alive",
                    value = uiState.defaultKeepAlive,
                    onValueChange = viewModel::setDefaultKeepAlive,
                    modifier = Modifier.weight(1f),
                    placeholder = "5m"
                )
            }

            // Behavior
            SectionHeader("Behavior")
            SettingsSwitch(
                label = "Stream responses",
                checked = uiState.streamEnabled,
                onCheckedChange = viewModel::setStreamEnabled
            )
            SettingsSwitch(
                label = "Auto-generate chat titles",
                checked = uiState.autoTitle,
                onCheckedChange = viewModel::setAutoTitle
            )

            // Network
            SectionHeader("Network")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsTextField(
                    label = "Connect Timeout (s)",
                    value = uiState.connectTimeout,
                    onValueChange = viewModel::setConnectTimeout,
                    modifier = Modifier.weight(1f)
                )
                SettingsTextField(
                    label = "Read Timeout (s)",
                    value = uiState.readTimeout,
                    onValueChange = viewModel::setReadTimeout,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, style = MaterialTheme.typography.bodySmall) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
