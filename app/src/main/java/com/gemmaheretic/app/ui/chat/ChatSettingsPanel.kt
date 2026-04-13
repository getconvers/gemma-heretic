package com.gemmaheretic.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gemmaheretic.app.domain.model.ChatConfig
import com.gemmaheretic.app.domain.model.ChatSession

@Composable
fun ChatSettingsPanel(
    session: ChatSession?,
    onUpdateConfig: (ChatConfig) -> Unit,
    onDismiss: () -> Unit
) {
    if (session == null) return

    var systemPrompt by remember(session.id) { mutableStateOf(session.systemPrompt ?: "") }
    var temperature by remember(session.id) { mutableStateOf(session.temperature?.toString() ?: "") }
    var topP by remember(session.id) { mutableStateOf(session.topP?.toString() ?: "") }
    var topK by remember(session.id) { mutableStateOf(session.topK?.toString() ?: "") }
    var numCtx by remember(session.id) { mutableStateOf(session.numCtx?.toString() ?: "") }
    var maxTokens by remember(session.id) { mutableStateOf(session.maxTokens?.toString() ?: "") }
    var repeatPenalty by remember(session.id) { mutableStateOf(session.repeatPenalty?.toString() ?: "") }
    var seed by remember(session.id) { mutableStateOf(session.seed?.toString() ?: "") }
    var keepAlive by remember(session.id) { mutableStateOf(session.keepAlive ?: "") }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Chat Settings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    TextButton(onClick = {
                        onUpdateConfig(
                            ChatConfig(
                                systemPrompt = systemPrompt.ifBlank { null },
                                temperature = temperature.toFloatOrNull(),
                                topP = topP.toFloatOrNull(),
                                topK = topK.toIntOrNull(),
                                numCtx = numCtx.toIntOrNull(),
                                maxTokens = maxTokens.toIntOrNull(),
                                repeatPenalty = repeatPenalty.toFloatOrNull(),
                                seed = seed.toIntOrNull(),
                                keepAlive = keepAlive.ifBlank { null }
                            )
                        )
                        onDismiss()
                    }) {
                        Text("Apply")
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
            }

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it },
                label = { Text("System Prompt") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactField("Temperature", temperature, { temperature = it }, Modifier.weight(1f))
                CompactField("Top P", topP, { topP = it }, Modifier.weight(1f))
                CompactField("Top K", topK, { topK = it }, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactField("Context Size", numCtx, { numCtx = it }, Modifier.weight(1f))
                CompactField("Max Tokens", maxTokens, { maxTokens = it }, Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactField("Repeat Penalty", repeatPenalty, { repeatPenalty = it }, Modifier.weight(1f))
                CompactField("Seed", seed, { seed = it }, Modifier.weight(1f))
                CompactField("Keep Alive", keepAlive, { keepAlive = it }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall
    )
}
