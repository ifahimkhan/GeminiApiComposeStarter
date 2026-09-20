package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import java.util.Locale

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val speechPrompt = stringResource(R.string.speech_prompt)

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            viewModel.onVoiceResult(spoken)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer(
                prompt = speechPrompt,
                launcher = { intent -> speechLauncher.launch(intent) },
                onUnavailable = viewModel::onSpeechUnavailable,
            )
        } else {
            viewModel.onMicPermissionDenied()
        }
    }

    ChatScreen(
        state = state,
        windowSizeClass = windowSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onRetry = viewModel::onRetry,
        onErrorShown = viewModel::onErrorShown,
        onVoiceClick = {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

            if (hasMicPermission) {
                launchSpeechRecognizer(
                    prompt = speechPrompt,
                    launcher = { intent -> speechLauncher.launch(intent) },
                    onUnavailable = viewModel::onSpeechUnavailable,
                )
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        onToggleAutoSendVoice = viewModel::onToggleAutoSendVoice,
        onToggleDynamicColor = viewModel::onToggleDynamicColor,
        onClearHistory = viewModel::onClearHistory,
    )
}

private fun launchSpeechRecognizer(
    prompt: String,
    launcher: (Intent) -> Unit,
    onUnavailable: () -> Unit,
) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
    }
    try {
        launcher(intent)
    } catch (_: ActivityNotFoundException) {
        onUnavailable()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    windowSizeClass: WindowSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onVoiceClick: () -> Unit = {},
    onToggleAutoSendVoice: () -> Unit = {},
    onToggleDynamicColor: () -> Unit = {},
    onClearHistory: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val layout = rememberChatLayout(windowSizeClass)

    val errorMessage = state.errorMessage
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = if (state.errorCanRetry) retryLabel else null,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            onErrorShown()
            if (result == SnackbarResult.ActionPerformed) {
                onRetry()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            ChatTopBar(
                autoSendVoice = state.autoSendVoice,
                dynamicColor = state.dynamicColor,
                clearEnabled = state.messages.isNotEmpty() && !state.isLoading,
                onToggleAutoSendVoice = onToggleAutoSendVoice,
                onToggleDynamicColor = onToggleDynamicColor,
                onClearHistory = onClearHistory,
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {

            Column(
                modifier = Modifier
                    .then(
                        if (layout.maxContentWidth != null) {
                            Modifier.widthIn(max = layout.maxContentWidth)
                        } else {
                            Modifier
                        }
                    )
                    .fillMaxSize()
                    .padding(
                        horizontal = layout.horizontalPadding,
                        vertical = layout.verticalPadding,
                    )
            ) {

                ConversationArea(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    bubbleMaxWidthFraction = layout.bubbleMaxWidthFraction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .heightIn(min = 0.dp),
                )

                state.configurationError?.let { message ->
                    ConfigurationErrorCard(
                        message = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                }

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = state.canSend,
                    inputMinLines = layout.inputMinLines,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceClick = onVoiceClick,
                )
            }

        }
    }
}

/** Sizing decisions derived from the window size class, recomputed only when it changes. */
@Immutable
private data class ChatLayout(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val maxContentWidth: Dp?,
    val bubbleMaxWidthFraction: Float,
    val inputMinLines: Int,
)

@Composable
private fun rememberChatLayout(windowSizeClass: WindowSizeClass): ChatLayout {
    val widthSizeClass = windowSizeClass.widthSizeClass
    val heightSizeClass = windowSizeClass.heightSizeClass

    return remember(widthSizeClass, heightSizeClass) {
        val isShort = heightSizeClass == WindowHeightSizeClass.Compact

        ChatLayout(
            horizontalPadding = when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> 16.dp
                WindowWidthSizeClass.Medium -> 32.dp
                else -> 48.dp
            },
            verticalPadding = if (isShort) 8.dp else 16.dp,
            maxContentWidth = when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> null
                WindowWidthSizeClass.Medium -> 720.dp
                else -> 960.dp
            },
            bubbleMaxWidthFraction = when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> 0.88f
                WindowWidthSizeClass.Medium -> 0.72f
                else -> 0.6f
            },
            // Landscape phones leave very little vertical room for the input field.
            inputMinLines = if (isShort) 1 else 3,
        )
    }
}

@Composable
private fun ConversationArea(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    bubbleMaxWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    if (messages.isEmpty() && !isLoading) {
        EmptyConversation(modifier = modifier)
        return
    }

    val listState = rememberLazyListState()
    val latestMessageId = messages.lastOrNull()?.id
    val lastIndex = messages.lastIndex + if (isLoading) 1 else 0

    LaunchedEffect(latestMessageId, isLoading) {
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = messages,
            key = { message -> message.id },
            contentType = { message -> message.isUser },
        ) { message ->
            ChatBubble(
                message = message,
                widthFraction = bubbleMaxWidthFraction,
            )
        }

        if (isLoading) {
            item(key = "gemini-loading", contentType = "loading") {
                LoadingBubble(widthFraction = bubbleMaxWidthFraction)
            }
        }
    }
}

@Composable
private fun EmptyConversation(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.empty_conversation_title),
            style = MaterialTheme.typography.titleMedium,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.response_placeholder),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    widthFraction: Float,
) {
    val text = remember(message.text, message.isUser) {
        if (message.isUser) AnnotatedString(message.text) else message.text.toBoldAnnotatedString()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .align(
                    if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ),
            verticalAlignment = Alignment.Top,
        ) {

            if (!message.isUser) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = stringResource(R.string.gemini),
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Surface(
                modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
            ),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (message.isUser) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            tonalElevation = 2.dp,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            )
            }
        }
    }
}

@Composable
private fun LoadingBubble(widthFraction: Float) {
    val waitingLabel = stringResource(R.string.waiting_for_response)

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = stringResource(R.string.gemini),
                modifier = Modifier
                    .padding(top = 8.dp, end = 8.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomEnd = 16.dp,
                    bottomStart = 4.dp,
                ),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .semantics { contentDescription = waitingLabel },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = waitingLabel,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurationErrorCard(
    message: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
            )

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = stringResource(R.string.configuration_error_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    autoSendVoice: Boolean,
    dynamicColor: Boolean,
    clearEnabled: Boolean,
    onToggleAutoSendVoice: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onClearHistory: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.gemini)) },
        actions = {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.auto_send_voice)) },
                        onClick = onToggleAutoSendVoice,
                        trailingIcon = {
                            Checkbox(
                                checked = autoSendVoice,
                                onCheckedChange = null,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.dynamic_color)) },
                        onClick = onToggleDynamicColor,
                        trailingIcon = {
                            Checkbox(
                                checked = dynamicColor,
                                onCheckedChange = null,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clear_history)) },
                        onClick = {
                            menuExpanded = false
                            onClearHistory()
                        },
                        enabled = clearEnabled,
                    )
                }
            }
        },
    )
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    inputMinLines: Int,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            label = {
                Text(stringResource(R.string.enter_your_prompt_here))
            },
            minLines = inputMinLines,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                {
                    Text(stringResource(R.string.field_cannot_be_empty))
                }
            },
        )

        OutlinedIconButton(
            onClick = onVoiceClick,
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = stringResource(R.string.voice_input),
            )
        }

        Spacer(Modifier.width(8.dp))

        FilledIconButton(
            onClick = onSend,
            enabled = enabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private val PhoneWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(411.dp, 891.dp))

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
private val TabletWindowSizeClass = WindowSizeClass.calculateFromSize(DpSize(1024.dp, 800.dp))

@Preview(showBackground = true, name = "Phone")
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(messages = PreviewMessages),
            windowSizeClass = PhoneWindowSizeClass,
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, name = "Tablet", widthDp = 1024, heightDp = 800)
@Composable
private fun ChatScreenTabletPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(messages = PreviewMessages),
            windowSizeClass = TabletWindowSizeClass,
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty")
@Composable
private fun ChatScreenEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(),
            windowSizeClass = PhoneWindowSizeClass,
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true, name = "Missing key")
@Composable
private fun ChatScreenConfigurationErrorPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                configurationError = ChatViewModel.MISSING_API_KEY_MESSAGE,
            ),
            windowSizeClass = PhoneWindowSizeClass,
            onPromptChange = {},
            onSend = {},
            onRetry = {},
            onErrorShown = {},
        )
    }
}

private val PreviewMessages = listOf(
    ChatMessage(
        id = "1",
        text = "Hello!",
        isUser = true,
    ),
    ChatMessage(
        id = "2",
        text = "Hello! How can I help you?",
        isUser = false,
    ),
    ChatMessage(
        id = "3",
        text = "Explain Kotlin.",
        isUser = true,
    ),
    ChatMessage(
        id = "4",
        text = "**Kotlin** is a modern programming language commonly used for Android development.",
        isUser = false,
    ),
)
