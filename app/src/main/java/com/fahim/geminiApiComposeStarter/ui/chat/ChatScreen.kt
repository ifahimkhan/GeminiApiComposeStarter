package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    widthSizeClass: WindowWidthSizeClass,
) {
    // State is collected here and passed down, so all composables below are stateless
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Voice input: opens Android's speech-to-text screen and puts the result in the input box
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onPromptChange(spokenText)
            }
        }
    }

    val onMicClick: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.onVoiceUnavailable()
        }
    }

    ChatScreen(
        state = state,
        isWideScreen = widthSizeClass != WindowWidthSizeClass.Compact,
        isDarkMode = state.darkMode ?: isSystemInDarkTheme(),
        onDarkModeChange = viewModel::onDarkModeChange,
        onClearChat = viewModel::onClearChat,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onErrorShown = viewModel::onErrorShown,
        onMicClick = onMicClick,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    isWideScreen: Boolean,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onErrorShown: () -> Unit,
    onMicClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Show a Snackbar when something fails
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // Auto-scroll to the latest message (or the loading bubble)
    val itemCount = state.messages.size + if (state.isLoading) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    // Tablets / landscape: keep the chat centred and not too wide; bubbles can be a bit wider
    val contentMaxWidth: Dp = if (isWideScreen) 720.dp else Dp.Unspecified
    val bubbleMaxWidth: Dp = if (isWideScreen) 520.dp else 280.dp

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                HeaderBar(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    onClearChat = onClearChat,
                    canClear = state.messages.isNotEmpty() && !state.isLoading,
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (state.messages.isEmpty() && !state.isLoading) {
                        Text(
                            text = stringResource(R.string.response_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(items = state.messages, key = { it.id }) { message ->
                            MessageBubble(message = message, maxWidth = bubbleMaxWidth)
                        }
                        if (state.isLoading) {
                            item(key = "loading") { LoadingBubble() }
                        }
                    }
                }
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                )
            }
        }
    }
}

@Composable
private fun HeaderBar(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onClearChat: () -> Unit,
    canClear: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Chat by Manvi N146",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        Text(text = "Dark", style = MaterialTheme.typography.labelLarge)
        Switch(
            checked = isDarkMode,
            onCheckedChange = onDarkModeChange,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(onClick = onClearChat, enabled = canClear) {
            Icon(imageVector = Icons.Filled.Delete, contentDescription = "Clear chat")
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, maxWidth: Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!message.isUser) {
            AssistantIcon()
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp,
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (message.isUser) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            if (message.isUser) {
                Text(text = message.text, fontSize = 16.sp, modifier = Modifier.padding(12.dp))
            } else {
                Text(
                    text = message.text.toBoldAnnotatedString(),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistantIcon()
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.padding(12.dp).size(24.dp),
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
private fun AssistantIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_assistant),
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp).size(32.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMicClick, enabled = enabled) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Voice input",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            maxLines = 4,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )
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
                    ChatMessage(id = 0, text = "Hi Gemini!", isUser = true),
                    ChatMessage(id = 1, text = "**Hello!** How can I help you today?", isUser = false),
                ),
            ),
            isWideScreen = false,
            isDarkMode = false,
            onDarkModeChange = {},
            onClearChat = {},
            onPromptChange = {},
            onSend = {},
            onErrorShown = {},
            onMicClick = {},
        )
    }
}