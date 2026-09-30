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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

object ChatTestTags {
    const val MESSAGE_LIST = "message_list"
    const val PROMPT_INPUT = "prompt_input"
    const val SEND_BUTTON = "send_button"
    const val MIC_BUTTON = "mic_button"
    const val LOADING = "loading_indicator"
}

/** Stateful entry point: the only composable that knows about the ViewModel. */
@Composable
fun ChatRoute(viewModel: ChatViewModel, widthSizeClass: WindowWidthSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceResult = viewModel::onVoiceResult,
        onVoiceUnavailable = viewModel::onVoiceUnavailable,
        onErrorShown = viewModel::onErrorShown,
        onClearChat = viewModel::onClearChat,
        onToggleTheme = viewModel::onToggleTheme,
    )
}

/** Stateless screen: everything comes in as parameters, events go out as lambdas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
    onErrorShown: () -> Unit,
    onClearChat: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // Responsive sizing: phones use full width, tablets/landscape get a centred column.
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val contentMaxWidth: Dp = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> Dp.Unspecified
        WindowWidthSizeClass.Medium -> 640.dp
        else -> 840.dp
    }
    val bubbleMaxWidth: Dp = if (isCompact) 300.dp else 520.dp

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_title)) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = when (state.themeMode) {
                                ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Filled.LightMode
                                ThemeMode.DARK -> Icons.Filled.DarkMode
                            },
                            contentDescription = stringResource(R.string.toggle_theme),
                        )
                    }
                    IconButton(onClick = onClearChat, enabled = state.messages.isNotEmpty()) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_chat))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxSize()
                    .padding(horizontal = if (isCompact) 12.dp else 24.dp, vertical = 8.dp),
            ) {
                MessageList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    bubbleMaxWidth = bubbleMaxWidth,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceResult = onVoiceResult,
                    onVoiceUnavailable = onVoiceUnavailable,
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    bubbleMaxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0

    // Auto-scroll to the newest item whenever a message arrives or loading starts.
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    if (messages.isEmpty() && !isLoading) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.response_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.testTag(ChatTestTags.MESSAGE_LIST),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(items = messages, key = { it.id }) { message ->
                ChatBubble(message = message, maxWidth = bubbleMaxWidth)
            }
            if (isLoading) {
                item(key = "loading") { LoadingBubble() }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, maxWidth: Dp) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Text(
                text = message.text.toBoldAnnotatedString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            CircularProgressIndicator(
                modifier = Modifier.padding(12.dp).size(24.dp).testTag(ChatTestTags.LOADING),
                strokeWidth = 3.dp,
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
    onVoiceResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
) {
    val speechPrompt = stringResource(R.string.speak_now)
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(onVoiceResult)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 8.dp).testTag(ChatTestTags.PROMPT_INPUT),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            maxLines = 4,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let { { Text(stringResource(R.string.field_cannot_be_empty)) } },
        )
        IconButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
                }
                try {
                    voiceLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    onVoiceUnavailable()
                }
            },
            enabled = enabled,
            modifier = Modifier.testTag(ChatTestTags.MIC_BUTTON),
        ) {
            Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.voice_input))
        }
        FilledIconButton(
            onClick = onSend,
            enabled = enabled,
            modifier = Modifier.testTag(ChatTestTags.SEND_BUTTON),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
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
                    ChatMessage(1, "What is Jetpack Compose?", isUser = true),
                    ChatMessage(2, "**Jetpack Compose** is Android's modern UI toolkit.", isUser = false),
                ),
            ),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {}, onSend = {}, onVoiceResult = {}, onVoiceUnavailable = {},
            onErrorShown = {}, onClearChat = {}, onToggleTheme = {},
        )
    }
}
