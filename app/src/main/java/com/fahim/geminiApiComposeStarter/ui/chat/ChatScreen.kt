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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

/** Stateful entry point: collects state from the ViewModel and wires events back to it. */
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onRetry = viewModel::onRetry,
        onErrorShown = viewModel::onErrorShown,
        onSuggestionClick = viewModel::onSuggestionClick,
        onVoiceResult = viewModel::onVoiceResult,
        onClearChat = viewModel::onClearChat,
        onThemeModeChange = viewModel::onThemeModeChange,
    )
}

/** Stateless screen: everything it shows comes from [state]; everything the user does goes out as a callback. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onVoiceResult: (String) -> Unit,
    onClearChat: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var showClearDialog by remember { mutableStateOf(false) }

    val retryLabel = stringResource(R.string.retry)
    val copiedText = stringResource(R.string.copied)
    val voiceUnavailable = stringResource(R.string.voice_unavailable)
    val voicePrompt = stringResource(R.string.voice_prompt)

    // API failures surface as a Snackbar with a Retry action.
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = retryLabel,
            duration = SnackbarDuration.Long,
        )
        onErrorShown()
        if (result == SnackbarResult.ActionPerformed) onRetry()
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) onVoiceResult(spoken)
        }
    }
    val onMicClick: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(voiceUnavailable) }
        }
    }

    val onCopy: (String) -> Unit = { text ->
        clipboard.setText(AnnotatedString(text))
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(copiedText, duration = SnackbarDuration.Short)
        }
    }

    val contentMaxWidth = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> Dp.Unspecified
        WindowWidthSizeClass.Medium -> 640.dp
        else -> 840.dp
    }

    val scheme = MaterialTheme.colorScheme
    // Blend to an opaque colour (not a translucent one) so the window's own background never shows through.
    val blend = if (scheme.background.luminance() < 0.5f) 0.35f else 0.55f
    val gradientEnd = lerp(scheme.background, scheme.primaryContainer, blend)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.background, gradientEnd))),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GeminiAvatar(size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.chat_title), fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        val (themeIcon, themeLabel) = when (state.themeMode) {
                            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto to R.string.theme_system
                            ThemeMode.LIGHT -> Icons.Filled.LightMode to R.string.theme_light
                            ThemeMode.DARK -> Icons.Filled.DarkMode to R.string.theme_dark
                        }
                        IconButton(onClick = { onThemeModeChange(state.themeMode.next()) }) {
                            Icon(themeIcon, contentDescription = stringResource(themeLabel))
                        }
                        IconButton(onClick = { showClearDialog = true }, enabled = state.messages.isNotEmpty()) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.clear_chat))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = scheme.onBackground,
                        actionIconContentColor = scheme.primary,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.weight(1f).widthIn(max = contentMaxWidth).fillMaxWidth()) {
                    if (state.messages.isEmpty() && !state.isLoading) {
                        EmptyState(onSuggestionClick = onSuggestionClick)
                    } else {
                        MessageList(
                            messages = state.messages,
                            isLoading = state.isLoading,
                            widthSizeClass = widthSizeClass,
                            onCopy = onCopy,
                        )
                    }
                }
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    sendEnabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                    modifier = Modifier.widthIn(max = contentMaxWidth),
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.clear_chat_title)) },
            text = { Text(stringResource(R.string.clear_chat_message)) },
            confirmButton = {
                TextButton(onClick = { showClearDialog = false; onClearChat() }) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    widthSizeClass: WindowWidthSizeClass,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0

    // Keep the newest message (or the typing bubble) in view.
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fraction = if (widthSizeClass == WindowWidthSizeClass.Compact) 0.85f else 0.7f
        val maxBubbleWidth = maxWidth * fraction - AvatarSlot

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items = messages, key = { it.id }) { message ->
                MessageBubble(message = message, maxBubbleWidth = maxBubbleWidth, onCopy = onCopy)
            }
            if (isLoading) {
                item(key = "typing") { TypingBubble() }
            }
        }
    }
}

private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.SYSTEM -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK -> ThemeMode.SYSTEM
}

private val PreviewMessages = listOf(
    ChatMessage(1, "Explain Kotlin coroutines simply", isUser = true, timestamp = 1_700_000_000_000),
    ChatMessage(2, "**Coroutines** are lightweight threads that let you write async code like normal code.", isUser = false, timestamp = 1_700_000_005_000),
    ChatMessage(3, "Nice, thanks!", isUser = true, timestamp = 1_700_000_060_000),
)

@Preview(showBackground = true, name = "Conversation")
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(messages = PreviewMessages, isLoading = true),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {}, onSend = {}, onRetry = {}, onErrorShown = {},
            onSuggestionClick = {}, onVoiceResult = {}, onClearChat = {}, onThemeModeChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty dark")
@Composable
private fun ChatScreenEmptyDarkPreview() {
    GeminiApiComposeStarterTheme(themeMode = ThemeMode.DARK) {
        ChatScreen(
            state = ChatUiState(themeMode = ThemeMode.DARK),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onPromptChange = {}, onSend = {}, onRetry = {}, onErrorShown = {},
            onSuggestionClick = {}, onVoiceResult = {}, onClearChat = {}, onThemeModeChange = {},
        )
    }
}
