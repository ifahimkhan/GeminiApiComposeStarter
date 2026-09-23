package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel, state: ChatUiState, windowSizeClass: WindowSizeClass) {
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.trim()
            if (!text.isNullOrEmpty()) viewModel.onPromptChange(text)
            else viewModel.showError("No speech was recognized. Please try again or type your message.")
        }
    }
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoice = {
            try {
                speechLauncher.launch(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message to Gemini")
                    },
                )
            } catch (error: ActivityNotFoundException) {
                viewModel.showError("Speech recognition is unavailable on this device. Please type your message.")
            } catch (error: SecurityException) {
                viewModel.showError("Speech recognition could not start. Please type your message.")
            }
        },
        onThemeChange = viewModel::setTheme,
        onClearRequest = viewModel::requestClearHistory,
        onClearConfirm = viewModel::clearHistory,
        onClearDismiss = viewModel::dismissClearHistory,
        onErrorDismiss = viewModel::dismissError,
        widthClass = windowSizeClass.widthSizeClass,
        compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit = {},
    onThemeChange: (ThemeMode) -> Unit = {},
    onClearRequest: () -> Unit = {},
    onClearConfirm: () -> Unit = {},
    onClearDismiss: () -> Unit = {},
    onErrorDismiss: () -> Unit = {},
    widthClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    compactHeight: Boolean = false,
) {
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val busy = state.isLoading || state.isInitializing || state.isClearing
    val wide = widthClass != WindowWidthSizeClass.Compact
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            onErrorDismiss()
        }
    }
    LaunchedEffect(state.messages.lastOrNull()?.id, state.isLoading) {
        val count = state.messages.size + if (state.isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    if (state.showClearConfirmation) {
        AlertDialog(
            onDismissRequest = onClearDismiss,
            title = { Text("Clear conversation?") },
            text = { Text("This removes all saved messages from this device.") },
            confirmButton = { TextButton(onClick = onClearConfirm) { Text("Clear messages") } },
            dismissButton = { TextButton(onClick = onClearDismiss) { Text("Cancel") } },
        )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = if (wide) 960.dp else 600.dp)
                    .fillMaxSize().padding(horizontal = if (wide) 32.dp else 16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text("Gemini chat", style = MaterialTheme.typography.headlineSmall)
                        if (!compactHeight) Text(
                            "Ask, explore, and keep your ideas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onClearRequest, enabled = !busy && state.messages.isNotEmpty()) {
                        Text("Clear history")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = state.theme == mode,
                            onClick = { onThemeChange(mode) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) },
                        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (state.messages.isEmpty() && !state.isLoading) {
                        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Start a conversation", style = MaterialTheme.typography.titleLarge)
                            Text("Type a question or use Voice. Your history stays on this device.", Modifier.padding(top = 8.dp))
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("conversation"),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.messages, key = { it.id }, contentType = { it.isUser }) { message ->
                            ChatBubble(message, wide)
                        }
                        if (state.isLoading) item(key = "loading") {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CircularProgressIndicator(Modifier.size(24.dp).testTag("response_progress"))
                                Text("Gemini is thinking…")
                            }
                        }
                    }
                    if (state.isInitializing || state.isClearing) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = onPromptChange,
                    modifier = Modifier.fillMaxWidth().testTag("prompt"),
                    label = { Text("Message Gemini") },
                    minLines = 1,
                    maxLines = if (compactHeight) 2 else 4,
                    enabled = !busy,
                    isError = state.promptError != null,
                    supportingText = if (state.promptError != null) ({ Text("Enter a message first") }) else null,
                )
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onVoice, enabled = !busy) { Text("Voice") }
                    Button(onClick = onSend, enabled = !busy) { Text("Send") }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, wide: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (wide) 0.78f else 0.9f).testTag("message_" + message.id),
            shape = MaterialTheme.shapes.large,
            color = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(if (message.isUser) "You" else "Gemini", style = MaterialTheme.typography.labelMedium)
                SelectionContainer {
                    Text(message.text.toBoldAnnotatedString(), Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
