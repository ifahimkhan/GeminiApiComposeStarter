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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

/** Test tags shared with the Compose UI tests. */
object ChatTestTags {
    const val MESSAGE_LIST = "message_list"
    const val PROMPT_FIELD = "prompt_field"
    const val SEND_BUTTON = "send_button"
    const val MIC_BUTTON = "mic_button"
    const val LOADING = "loading_indicator"
    const val MENU_BUTTON = "menu_button"
}

/**
 * Stateful entry point: collects the ViewModel state and owns the Android-specific voice
 * launcher. Everything below this function is stateless and driven purely by [ChatUiState].
 */
@Composable
fun ChatRoute(viewModel: ChatViewModel, widthSizeClass: WindowWidthSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(viewModel::onVoiceResult)
        }
    }
    val speakPrompt = stringResource(R.string.speak_prompt)

    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onMicClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, speakPrompt)
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                viewModel.onVoiceUnavailable()
            }
        },
        onErrorShown = viewModel::onErrorShown,
        onClearChat = viewModel::onClearChat,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    onErrorShown: () -> Unit,
    onClearChat: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    // Adaptive sizing: wider windows get wider bubbles, and the column never grows past 840dp
    // so lines stay readable on tablets and in landscape.
    val bubbleMaxWidth: Dp = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 300.dp
        WindowWidthSizeClass.Medium -> 460.dp
        else -> 600.dp
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    ChatMenu(
                        themeMode = state.themeMode,
                        dynamicColor = state.dynamicColor,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onClearChat = onClearChat,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    canSend = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                )
            }
        }
    }
}

@Composable
private fun ChatMenu(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onClearChat: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = { expanded = true },
        modifier = Modifier.testTag(ChatTestTags.MENU_BUTTON),
    ) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        val themes = listOf(
            ThemeMode.SYSTEM to R.string.theme_system,
            ThemeMode.LIGHT to R.string.theme_light,
            ThemeMode.DARK to R.string.theme_dark,
        )
        themes.forEach { (mode, label) ->
            DropdownMenuItem(
                text = { Text(stringResource(label)) },
                trailingIcon = { if (mode == themeMode) Icon(Icons.Default.Check, null) },
                onClick = { onThemeModeChange(mode) },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dynamic_colour)) },
            trailingIcon = { if (dynamicColor) Icon(Icons.Default.Check, null) },
            onClick = { onDynamicColorChange(!dynamicColor) },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clear_chat)) },
            onClick = {
                expanded = false
                onClearChat()
            },
        )
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

    // Auto-scroll to the newest message (or the loading bubble) whenever the list grows.
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag(ChatTestTags.MESSAGE_LIST),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (messages.isEmpty() && !isLoading) {
            item(key = "placeholder") {
                Text(
                    text = stringResource(R.string.response_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        // Stable keys (the Room row id) keep item state and animations correct as the list grows.
        items(items = messages, key = { it.id }) { message ->
            MessageBubble(message = message, maxWidth = bubbleMaxWidth)
        }
        if (isLoading) {
            item(key = "loading") { LoadingBubble() }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, maxWidth: Dp) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!isUser) AssistantAvatar()
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Text(
                text = if (isUser) AnnotatedString(message.text) else message.text.toBoldAnnotatedString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    val description = stringResource(R.string.gemini_is_typing)
    Row(verticalAlignment = Alignment.CenterVertically) {
        AssistantAvatar()
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp).testTag(ChatTestTags.LOADING),
            strokeWidth = 3.dp,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun AssistantAvatar() {
    Icon(
        painter = painterResource(R.drawable.ic_assistant),
        contentDescription = null,
        modifier = Modifier.padding(end = 8.dp, top = 6.dp).size(28.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    canSend: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 4.dp).testTag(ChatTestTags.PROMPT_FIELD),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = 1,
            maxLines = 4,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )
        IconButton(onClick = onMicClick, modifier = Modifier.testTag(ChatTestTags.MIC_BUTTON)) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.voice_input),
            )
        }
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.testTag(ChatTestTags.SEND_BUTTON),
        ) {
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
                    ChatMessage(1, "Hi Gemini!", isUser = true),
                    ChatMessage(2, "**Hello!** How can I help you today?", isUser = false),
                ),
            ),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {},
            onSend = {},
            onMicClick = {},
            onErrorShown = {},
            onClearChat = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
        )
    }
}
