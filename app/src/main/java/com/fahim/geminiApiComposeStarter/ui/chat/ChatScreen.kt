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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.ui.chat.components.MessageBubble
import com.fahim.geminiApiComposeStarter.ui.chat.components.SettingsContent
import com.fahim.geminiApiComposeStarter.ui.chat.components.TypingBubble
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import java.util.Locale

const val MESSAGE_LIST_TAG = "message_list"
const val PROMPT_FIELD_TAG = "prompt_field"
const val SEND_BUTTON_TAG = "send_button"
const val MIC_BUTTON_TAG = "mic_button"
const val EMPTY_STATE_TAG = "empty_state"

/**
 * Stateful entry point: collects the ViewModel state and owns the speech-to-text launcher.
 * Everything below [ChatScreen] is stateless.
 */
@Composable
fun ChatRoute(viewModel: ChatViewModel, windowSizeClass: WindowSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
    val speechPrompt = stringResource(R.string.voice_prompt)

    val actions = remember(viewModel, speechLauncher, speechPrompt) {
        ChatActions(
            onPromptChange = viewModel::onPromptChange,
            onSend = viewModel::onSend,
            onRetry = viewModel::onRetry,
            onErrorShown = viewModel::onErrorShown,
            onMicClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    viewModel.onVoiceUnavailable()
                }
            },
            onClearHistory = viewModel::onClearHistory,
            onOpenSettings = viewModel::onOpenSettings,
            onDismissSettings = viewModel::onDismissSettings,
            onThemeModeChange = viewModel::onThemeModeChange,
            onDynamicColorChange = viewModel::onDynamicColorChange,
            onResponseStyleChange = viewModel::onResponseStyleChange,
        )
    }

    val layout = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> ChatLayout.EXPANDED
        WindowWidthSizeClass.Medium -> ChatLayout.MEDIUM
        else -> ChatLayout.COMPACT
    }
    ChatScreen(
        state = state,
        actions = actions,
        layout = layout,
        compactHeight = windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    actions: ChatActions,
    layout: ChatLayout = ChatLayout.COMPACT,
    compactHeight: Boolean = false,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = state.error?.let { errorMessage(it) }
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(state.error) {
        if (errorText != null) {
            val result = snackbarHostState.showSnackbar(
                message = errorText,
                actionLabel = if (state.retryPrompt != null) retryLabel else null,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) actions.onRetry()
            actions.onErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_title)) },
                actions = {
                    IconButton(
                        onClick = actions.onClearHistory,
                        enabled = state.messages.isNotEmpty() && !state.isLoading,
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.clear_chat))
                    }
                    if (layout != ChatLayout.EXPANDED) {
                        IconButton(onClick = actions.onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tablets / large landscape screens: settings live in a permanent side pane.
            if (layout == ChatLayout.EXPANDED) {
                Surface(
                    modifier = Modifier.width(320.dp).fillMaxHeight(),
                    tonalElevation = 1.dp,
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                        Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
                        SettingsContent(
                            preferences = state.preferences,
                            onThemeModeChange = actions.onThemeModeChange,
                            onDynamicColorChange = actions.onDynamicColorChange,
                            onResponseStyleChange = actions.onResponseStyleChange,
                        )
                    }
                }
                VerticalDivider()
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // On wider screens keep the conversation at a readable width, centred.
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = if (layout == ChatLayout.COMPACT) Dp.Unspecified else 840.dp)
                        .fillMaxWidth(),
                ) {
                    MessageList(
                        messages = state.messages,
                        isLoading = state.isLoading,
                        sideInset = if (layout == ChatLayout.COMPACT) 48.dp else 120.dp,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    PromptBar(
                        prompt = state.prompt,
                        promptError = state.promptError,
                        enabled = !state.isLoading,
                        compactHeight = compactHeight,
                        onPromptChange = actions.onPromptChange,
                        onSend = actions.onSend,
                        onMicClick = actions.onMicClick,
                    )
                }
            }
        }
    }

    if (state.showSettings && layout != ChatLayout.EXPANDED) {
        AlertDialog(
            onDismissRequest = actions.onDismissSettings,
            confirmButton = {
                TextButton(onClick = actions.onDismissSettings) { Text(stringResource(R.string.done)) }
            },
            title = { Text(stringResource(R.string.settings)) },
            text = {
                SettingsContent(
                    preferences = state.preferences,
                    onThemeModeChange = actions.onThemeModeChange,
                    onDynamicColorChange = actions.onDynamicColorChange,
                    onResponseStyleChange = actions.onResponseStyleChange,
                )
            },
        )
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    sideInset: Dp,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0

    // Auto-scroll to the newest message (or the typing indicator).
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    if (itemCount == 0) {
        EmptyState(modifier)
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.testTag(MESSAGE_LIST_TAG),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(
                items = messages,
                key = { it.id },                 // stable keys: items are reused, not recreated
                contentType = { it.role },
            ) { message ->
                MessageBubble(message = message, sideInset = sideInset)
            }
            if (isLoading) {
                item(key = "typing", contentType = "typing") { TypingBubble() }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp).testTag(EMPTY_STATE_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    compactHeight: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).testTag(PROMPT_FIELD_TAG),
            placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
            maxLines = if (compactHeight) 2 else 5,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
            shape = RoundedCornerShape(24.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = onMicClick,
            enabled = enabled,
            modifier = Modifier.testTag(MIC_BUTTON_TAG),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = stringResource(R.string.voice_input),
            )
        }
        FilledIconButton(
            onClick = onSend,
            enabled = enabled,
            modifier = Modifier.testTag(SEND_BUTTON_TAG),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}

@Composable
private fun errorMessage(error: ChatError): String = when (error) {
    ChatError.MissingApiKey -> stringResource(R.string.error_missing_key)
    ChatError.VoiceUnavailable -> stringResource(R.string.error_voice_unavailable)
    is ChatError.Request -> error.detail?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.error_request_with_detail, it) }
        ?: stringResource(R.string.error_request)
}

private val previewMessages = listOf(
    ChatMessage(1, Role.USER, "What is Jetpack Compose?", 0),
    ChatMessage(2, Role.MODEL, "**Jetpack Compose** is Android's modern toolkit for building native UI.", 0),
)

@Preview(showBackground = true, name = "Phone")
@Composable
private fun ChatScreenPhonePreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(state = ChatUiState(messages = previewMessages, isLoading = true), actions = ChatActions())
    }
}

@Preview(showBackground = true, name = "Tablet", widthDp = 1280, heightDp = 800)
@Composable
private fun ChatScreenTabletPreview() {
    GeminiApiComposeStarterTheme(darkTheme = true) {
        ChatScreen(
            state = ChatUiState(messages = previewMessages),
            actions = ChatActions(),
            layout = ChatLayout.EXPANDED,
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun ChatScreenEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(state = ChatUiState(), actions = ChatActions())
    }
}
