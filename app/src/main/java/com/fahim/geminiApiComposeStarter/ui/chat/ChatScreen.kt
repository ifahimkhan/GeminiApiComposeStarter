package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode

@Composable
fun ChatRoute(viewModel: ChatViewModel, widthSizeClass: WindowWidthSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.let(viewModel::onPromptChange)
        }
    }
    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onErrorShown = viewModel::onErrorShown,
        onCycleTheme = viewModel::onCycleTheme,
        onClearHistory = viewModel::onClearHistory,
        onVoiceRequested = {
            try {
                voiceLauncher.launch(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
                )
            } catch (_: ActivityNotFoundException) {
                viewModel.onVoiceUnavailable()
            }
        }
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onErrorShown: () -> Unit,
    onCycleTheme: () -> Unit,
    onClearHistory: () -> Unit,
    onVoiceRequested: () -> Unit
) {
    val snackbarHost = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val maxWidth = if (widthSizeClass == WindowWidthSizeClass.Compact) 680.dp else 840.dp
    LaunchedEffect(state.messages.size, state.isLoading) {
        if (state.messages.isNotEmpty() || state.isLoading) {
            listState.animateScrollToItem(state.messages.size - 1 + if (state.isLoading) 1 else 0)
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            onErrorShown()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = maxWidth).fillMaxSize().padding(horizontal = 16.dp)) {
                Text("Gemini Chat", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)) {
                    val themeLabel = when (state.themeMode) {
                        ThemeMode.SYSTEM -> "Theme: System"
                        ThemeMode.DARK -> "Theme: Dark"
                        ThemeMode.LIGHT -> "Theme: Light"
                    }
                    Button(onClick = onCycleTheme) { Text(themeLabel) }
                    Button(onClick = onClearHistory, enabled = state.messages.isNotEmpty()) {
                        Text("Clear chat")
                    }
                }
                if (!state.hasApiKey) {
                    Text(
                        "Add your Gemini API key to local.properties, then rebuild the app.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.messages.isEmpty()) {
                        item(key = "welcome") {
                            Text("Ask a question to start a conversation.", modifier = Modifier.padding(top = 24.dp))
                        }
                    }
                    items(state.messages, key = { it.id }) { message -> MessageBubble(message) }
                    if (state.isLoading) {
                        item(key = "loading") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator()
                                Text("Gemini is thinking…", modifier = Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.prompt,
                        onValueChange = onPromptChange,
                        label = { Text("Message") },
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        isError = state.promptError,
                        supportingText = if (state.promptError) ({ Text("Enter a message") }) else null
                    )
                    Button(onClick = onVoiceRequested, enabled = !state.isLoading) { Text("Voice") }
                    Button(onClick = onSend, enabled = !state.isLoading) { Text("Send") }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val color = if (message.fromUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (message.fromUser) Alignment.End else Alignment.Start
    Column(Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Text(if (message.fromUser) "You" else "Gemini", style = MaterialTheme.typography.labelMedium)
        Text(
            message.text,
            modifier = Modifier.widthIn(max = 620.dp)
                .background(color, RoundedCornerShape(18.dp))
                .padding(14.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
