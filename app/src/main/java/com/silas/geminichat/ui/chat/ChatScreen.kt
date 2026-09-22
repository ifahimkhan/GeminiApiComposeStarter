package com.silas.geminichat.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.silas.geminichat.model.ChatMessage
import com.silas.geminichat.model.MessageRole

@Composable
fun ChatRoute(viewModel: ChatViewModel, windowWidthSizeClass: WindowWidthSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        windowWidthSizeClass = windowWidthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceResult = viewModel::onVoiceResult,
        onClearHistory = viewModel::clearHistory,
        onUserNameChange = viewModel::onUserNameChange,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String?) -> Unit,
    onClearHistory: () -> Unit,
    onUserNameChange: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    if (showNameDialog) {
        NameDialog(
            initialName = state.userName,
            onDismiss = { showNameDialog = false },
            onConfirm = { name ->
                onUserNameChange(name.trim())
                showNameDialog = false
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ChatTopBar(
                userName = state.userName,
                onEditName = { showNameDialog = true },
                onClearHistory = onClearHistory,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .horizontalContentPadding(windowWidthSizeClass),
        ) {
            MessageList(
                messages = state.messages,
                isLoading = state.isLoading,
                windowWidthSizeClass = windowWidthSizeClass,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceResult = onVoiceResult,
            )
        }
    }
}

private fun Modifier.horizontalContentPadding(widthClass: WindowWidthSizeClass): Modifier =
    when (widthClass) {
        WindowWidthSizeClass.Compact -> padding(horizontal = 12.dp)
        WindowWidthSizeClass.Medium -> padding(horizontal = 32.dp)
        else -> padding(horizontal = 48.dp)
    }

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (messages.isEmpty() && !isLoading) {
            item { EmptyState() }
        }
        items(messages, key = { it.id }) { message ->
            MessageBubble(message = message, windowWidthSizeClass = windowWidthSizeClass)
        }
        if (isLoading) {
            item { TypingIndicator() }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, windowWidthSizeClass: WindowWidthSizeClass) {
    val isUser = message.role == MessageRole.USER
    val maxWidth = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 300.dp
        WindowWidthSizeClass.Medium -> 460.dp
        else -> 640.dp
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.padding(14.dp).size(20.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ask Gemini anything", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your conversation is saved on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String?) -> Unit,
) {
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        onVoiceResult(text)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        FilledTonalIconButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                }
                runCatching { speechLauncher.launch(intent) }
            },
            enabled = enabled,
        ) {
            Icon(Icons.Filled.Mic, contentDescription = "Voice input")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            label = { Text("Type your message") },
            maxLines = 4,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let { { Text("Message cannot be empty") } },
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(onClick = onSend, enabled = enabled && prompt.isNotBlank()) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    userName: String,
    onEditName: () -> Unit,
    onClearHistory: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text("Silas Gemini Chat", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = if (userName.isNotBlank()) "Hi, $userName" else "n091 · MBA Tech CE, E1",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onEditName) {
                Icon(Icons.Filled.Person, contentDescription = "Set your name")
            }
            IconButton(onClick = onClearHistory) {
                Icon(Icons.Filled.Delete, contentDescription = "Clear history")
            }
        },
    )
}

@Composable
private fun NameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
