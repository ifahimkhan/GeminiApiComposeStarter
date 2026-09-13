package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.Author
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import java.util.Locale

/** Test tags so the Compose UI tests can find nodes that have no user-visible text. */
object ChatTestTags {
    const val MESSAGE_LIST = "messageList"
    const val LOADING = "loadingIndicator"
    const val PROMPT_FIELD = "promptField"
}

/**
 * Stateful entry point: collects the ViewModel state and owns the activity-result plumbing
 * for speech input. Everything below this is a stateless composable.
 */
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    widthSizeClass: WindowWidthSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val voicePrompt = stringResource(R.string.voice_prompt)
    val voiceUnavailable = stringResource(R.string.voice_unavailable)
    val permissionDenied = stringResource(R.string.voice_permission_denied)

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(viewModel::onVoiceResult)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechLauncher.launch(speechIntent(voicePrompt))
        } else {
            viewModel.onLocalError(permissionDenied)
        }
    }

    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onRetry = viewModel::onRetry,
        onErrorShown = viewModel::onErrorShown,
        onClearConversation = viewModel::onClearConversation,
        onThemeModeChange = viewModel::onThemeModeChange,
        onVoiceClick = {
            when {
                !SpeechRecognizer.isRecognitionAvailable(context) ->
                    viewModel.onLocalError(voiceUnavailable)

                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED ->
                    speechLauncher.launch(speechIntent(voicePrompt))

                else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )
}

private fun speechIntent(prompt: String): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onClearConversation: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVoiceClick: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(state.error) {
        val error = state.error ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = error.message,
            actionLabel = if (error.retryable) retryLabel else null,
            withDismissAction = !error.retryable,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) onRetry()
        onErrorShown()
    }

    // On a phone the conversation uses the full width; on a tablet or in landscape it is
    // capped and centred so lines stay readable, and bubbles take a smaller share.
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact
    val bubbleWidthFraction = if (isCompact) 0.88f else 0.66f

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            ChatTopBar(
                themeMode = state.themeMode,
                canClear = state.messages.isNotEmpty(),
                onThemeModeChange = onThemeModeChange,
                onClearConversation = onClearConversation,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (isCompact) Dp.Unspecified else 720.dp)
                    .fillMaxSize()
            ) {
                if (state.isConversationEmpty) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    MessageList(
                        messages = state.messages,
                        isLoading = state.isLoading,
                        bubbleWidthFraction = bubbleWidthFraction,
                        modifier = Modifier.weight(1f),
                    )
                }
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    isLoading = state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceClick = onVoiceClick,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    themeMode: ThemeMode,
    canClear: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearConversation: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(stringResource(R.string.chat_title)) },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.theme_mode),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    ThemeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(stringResource(mode.labelRes())) },
                            onClick = {
                                onThemeModeChange(mode)
                                menuExpanded = false
                            },
                            trailingIcon = {
                                if (mode == themeMode) {
                                    Icon(Icons.Filled.Check, contentDescription = null)
                                }
                            },
                        )
                    }
                }
            }
            IconButton(onClick = onClearConversation, enabled = canClear) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.clear_chat),
                )
            }
        },
    )
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    bubbleWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Follow the tail of the conversation as turns arrive, including the typing indicator.
    LaunchedEffect(messages.size, isLoading) {
        val lastIndex = messages.size - 1 + if (isLoading) 1 else 0
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag(ChatTestTags.MESSAGE_LIST),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Stable keys let Compose reuse bubbles instead of rebuilding the list on each turn.
        items(
            items = messages,
            key = { message -> message.id },
            contentType = { message -> message.author },
        ) { message ->
            MessageBubble(message = message, widthFraction = bubbleWidthFraction)
        }
        if (isLoading) {
            item(key = "typing", contentType = "typing") { TypingBubble() }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, widthFraction: Float) {
    val isUser = message.author == Author.USER
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(widthFraction),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = stringResource(if (isUser) R.string.you else R.string.gemini),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Surface(
                shape = shape,
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = message.text.toBoldAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .testTag(ChatTestTags.LOADING),
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.thinking),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    isLoading: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        IconButton(onClick = onVoiceClick, enabled = !isLoading) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.voice_input),
            )
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp)
                .testTag(ChatTestTags.PROMPT_FIELD),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            maxLines = 5,
            enabled = !isLoading,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        FilledIconButton(onClick = onSend, enabled = !isLoading) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private val previewMessages = listOf(
    ChatMessage(1, "Explain state hoisting in one line.", Author.USER, 1L),
    ChatMessage(
        2,
        "**State hoisting** moves state to the caller, leaving the composable stateless and reusable.",
        Author.MODEL,
        2L,
    ),
)

@Preview(name = "Phone", showBackground = true)
@Preview(name = "Phone dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme(dynamicColor = false) {
        ChatScreen(
            state = ChatUiState(messages = previewMessages, prompt = "And recomposition?"),
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onErrorShown = {},
            onClearConversation = {},
            onThemeModeChange = {},
            onVoiceClick = {},
        )
    }
}

@Preview(name = "Tablet", showBackground = true, widthDp = 840, heightDp = 620)
@Composable
private fun ChatScreenTabletPreview() {
    GeminiApiComposeStarterTheme(dynamicColor = false) {
        ChatScreen(
            state = ChatUiState(messages = previewMessages, isLoading = true),
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onErrorShown = {},
            onClearConversation = {},
            onThemeModeChange = {},
            onVoiceClick = {},
            widthSizeClass = WindowWidthSizeClass.Expanded,
        )
    }
}
