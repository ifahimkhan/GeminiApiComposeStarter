package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.Sender

@Composable
fun ChatRoute(viewModel: ChatViewModel, isExpanded: Boolean = false, darkTheme: Boolean = isSystemInDarkTheme(), onThemeToggle: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(state, viewModel::onPromptChange, viewModel::onSend, isExpanded, darkTheme, onThemeToggle, viewModel::onVoiceInputError)
}

@Composable
fun ChatScreen(state: ChatUiState, onPromptChange: (String) -> Unit, onSend: () -> Unit, isExpanded: Boolean = false, darkTheme: Boolean = false, onThemeToggle: () -> Unit = {}, onVoiceInputError: () -> Unit = {}) {
    val snackbarHost = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbarHost.showSnackbar(it) } }
    LaunchedEffect(state.messages.size, state.isLoading) {
        val last = state.messages.lastIndex
        if (last >= 0) listState.animateScrollToItem(last)
    }
    Scaffold(modifier = Modifier.fillMaxSize().imePadding(), snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxSize().widthIn(max = if (isExpanded) 840.dp else Dp.Unspecified).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Gemini Chat", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                IconButton(onClick = onThemeToggle, modifier = Modifier.semantics { contentDescription = "Toggle dark mode" }) {
                    Icon(Icons.Filled.DarkMode, "Toggle dark mode")
                }
            }
            if (state.messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Start a conversation with Gemini", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { items(state.messages.size, key = { state.messages[it].id }) { MessageBubble(state.messages[it]) } }
            }
            if (state.isLoading) Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp)); Text(" Gemini is thinking…")
            }
            PromptBar(state.prompt, state.promptError, !state.isLoading, onPromptChange, onSend, onVoiceInputError)
        }
        }
    }
}

@Composable private fun MessageBubble(message: ChatMessage) {
    val isUser = message.sender == Sender.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Text(
            message.text,
            modifier = Modifier.background(if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium).padding(12.dp),
            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = if (isUser) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable private fun PromptBar(prompt: String, error: PromptError?, enabled: Boolean, onChange: (String) -> Unit, onSend: () -> Unit, onVoiceInputError: () -> Unit) {
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(onChange) ?: onVoiceInputError()
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = prompt, onValueChange = onChange, modifier = Modifier.weight(1f).semantics { contentDescription = "Message input" },
            label = { Text("Message") }, enabled = enabled, isError = error != null, singleLine = false,
            supportingText = if (error != null) {{ Text("Message cannot be empty") }} else null,
        )
        FilledIconButton(onClick = {
            try { speechLauncher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
            }) } catch (_: Exception) { onVoiceInputError() }
        }, enabled = enabled, modifier = Modifier.padding(start = 4.dp).semantics { contentDescription = "Voice input" }) {
            Icon(Icons.Filled.Mic, "Voice input")
        }
        FilledIconButton(onClick = onSend, enabled = enabled, modifier = Modifier.padding(start = 4.dp).semantics { contentDescription = "Send message" }) {
            Icon(Icons.AutoMirrored.Filled.Send, "Send message")
        }
    }
}
