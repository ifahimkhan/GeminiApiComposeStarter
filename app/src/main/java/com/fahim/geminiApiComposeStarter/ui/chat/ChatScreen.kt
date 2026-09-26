package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        darkModeOverride = darkModeOverride,
        onToggleDarkMode = viewModel::toggleDarkMode,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceInputUnavailable = viewModel::onVoiceInputUnavailable,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    darkModeOverride: Boolean?,
    onToggleDarkMode: (currentlyDark: Boolean) -> Unit,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInputUnavailable: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val isDark = darkModeOverride ?: isSystemInDarkTheme()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Text(stringResource(R.string.dark_mode))
                    Switch(
                        checked = isDark,
                        onCheckedChange = { onToggleDarkMode(isDark) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.messages.isEmpty()) {
                    Text(
                        text = stringResource(R.string.response_placeholder),
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    MessageList(messages = state.messages, listState = listState)
                }
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                    )
                }
            }
            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceInputUnavailable = onVoiceInputUnavailable,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    // Wider bubbles on phones, narrower on tablets so lines don't stretch edge to edge.
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val bubbleMaxWidth: Dp = if (screenWidthDp >= 600) (screenWidthDp * 0.6f).dp else (screenWidthDp * 0.8f).dp

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = messages, key = { it.id }) { msg ->
            MessageBubble(msg = msg, maxWidth = bubbleMaxWidth)
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage, maxWidth: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isFromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (msg.isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Text(
                text = msg.text.toBoldAnnotatedString(),
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInputUnavailable: () -> Unit,
) {
    val context = LocalContext.current
    val voiceInputDescription = stringResource(R.string.voice_input)
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (recognized != null) onPromptChange(recognized)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = 3,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )
        IconButton(
            enabled = enabled,
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                }
                if (intent.resolveActivity(context.packageManager) != null) {
                    voiceLauncher.launch(intent)
                } else {
                    onVoiceInputUnavailable()
                }
            },
        ) {
            Text(text = "🎤", modifier = Modifier.semantics { contentDescription = voiceInputDescription })
        }
        FilledIconButton(onClick = onSend, enabled = enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(id = 0, text = "What is Jetpack Compose?", isFromUser = true),
                    ChatMessage(id = 1, text = "**Jetpack Compose** is Android's modern UI toolkit.", isFromUser = false),
                ),
            ),
            darkModeOverride = null,
            onToggleDarkMode = {},
            onPromptChange = {},
            onSend = {},
            onVoiceInputUnavailable = {},
        )
    }
}
