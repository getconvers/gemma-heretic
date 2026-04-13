package com.gemmaheretic.app.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gemmaheretic.app.domain.model.ChatMessage
import com.gemmaheretic.app.domain.model.MessageRole
import com.gemmaheretic.app.domain.model.StreamState
import com.gemmaheretic.app.ui.components.MarkdownText
import com.gemmaheretic.app.ui.theme.UserBubble
import com.gemmaheretic.app.ui.theme.UserBubbleLight
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    isDarkTheme: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    // Auto-scroll to bottom on new content
    val messageCount = uiState.messages.size
    val isStreaming = uiState.streamState == StreamState.Streaming
    LaunchedEffect(messageCount, uiState.currentStreamContent) {
        if (messageCount > 0 || isStreaming) {
            val targetIndex = if (isStreaming) messageCount else messageCount - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.session?.title ?: "Chat",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${uiState.session?.modelName ?: ""}${uiState.endpoint?.let { " · ${it.name}" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Model selector
                    var showModelMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = {
                            viewModel.loadModels()
                            showModelMenu = true
                        }) {
                            Icon(Icons.Default.Memory, contentDescription = "Change model")
                        }
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            if (uiState.isLoadingModels) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else {
                                uiState.models.forEach { model ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(model.name, style = MaterialTheme.typography.bodyMedium)
                                                if (model.parameterSize.isNotBlank()) {
                                                    Text(
                                                        model.parameterSize,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.updateModel(model.name)
                                            showModelMenu = false
                                        },
                                        leadingIcon = {
                                            if (model.name == uiState.session?.modelName) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // More options
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Chat Settings") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleSettings()
                                },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearChat()
                                },
                                leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                input = uiState.inputText,
                onInputChange = viewModel::updateInput,
                onSend = viewModel::sendMessage,
                onStop = viewModel::stopGeneration,
                isStreaming = uiState.streamState == StreamState.Streaming
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Chat settings panel
            if (uiState.showSettings) {
                ChatSettingsPanel(
                    session = uiState.session,
                    onUpdateConfig = viewModel::updateSessionConfig,
                    onDismiss = viewModel::toggleSettings
                )
            }

            // Error banner
            uiState.error?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        IconButton(
                            onClick = viewModel::dismissError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Messages
            if (uiState.messages.isEmpty() && !isStreaming) {
                EmptyChatState(
                    modelName = uiState.session?.modelName ?: "",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(
                            message = message,
                            isDarkTheme = isDarkTheme,
                            onEdit = { newContent -> viewModel.editAndResend(message.id, newContent) },
                            onRegenerate = if (message == uiState.messages.lastOrNull() && message.role == MessageRole.ASSISTANT) {
                                { viewModel.regenerate() }
                            } else null
                        )
                    }

                    // Streaming indicator
                    if (isStreaming) {
                        item(key = "streaming") {
                            StreamingBubble(
                                content = uiState.currentStreamContent,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modelName: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Start a conversation",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        if (modelName.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "with $modelName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    onEdit: (String) -> Unit,
    onRegenerate: (() -> Unit)?
) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.content) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Role label
        Text(
            text = if (isUser) "You" else "Assistant",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                if (isDarkTheme) UserBubble else UserBubbleLight
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (isEditing) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            isEditing = false
                            onEdit(editText)
                        }) {
                            Text("Resend")
                        }
                    }
                } else {
                    MarkdownText(
                        text = message.content,
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }

        // Action row
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Copy
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // Edit (user messages only)
            if (isUser) {
                IconButton(
                    onClick = {
                        editText = message.content
                        isEditing = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Regenerate (last assistant message only)
            if (onRegenerate != null) {
                IconButton(
                    onClick = onRegenerate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Duration info
            message.durationMs?.let { duration ->
                val seconds = duration / 1000.0
                Text(
                    text = "${String.format("%.1f", seconds)}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp)
                )
            }

            message.tokenCount?.let { count ->
                Text(
                    text = "${count}t",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
private fun StreamingBubble(
    content: String,
    isDarkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "Assistant",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
        )

        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (content.isNotEmpty()) {
                    MarkdownText(text = content, isDarkTheme = isDarkTheme)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    isStreaming: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message...") },
                maxLines = 6,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
            )

            if (isStreaming) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
            } else {
                FilledIconButton(
                    onClick = onSend,
                    modifier = Modifier.size(48.dp),
                    enabled = input.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}
