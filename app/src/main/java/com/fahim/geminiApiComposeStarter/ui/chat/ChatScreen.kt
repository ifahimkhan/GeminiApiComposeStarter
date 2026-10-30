package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        windowSizeClass = windowSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onDismissError = viewModel::onDismissError,
        onRetry = viewModel::onRetry,
        onShowSpeechError = viewModel::onShowSpeechError,
        onShowClearHistoryDialog = viewModel::onShowClearHistoryDialog,
        onConfirmClearHistory = viewModel::onConfirmClearHistory,
        onShowSettingsDialog = viewModel::onShowSettingsDialog,
        onSetThemeMode = viewModel::onSetThemeMode,
        onSetDynamicColor = viewModel::onSetDynamicColor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    windowSizeClass: WindowSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit,
    onShowSpeechError: (String) -> Unit,
    onShowClearHistoryDialog: (Boolean) -> Unit,
    onConfirmClearHistory: () -> Unit,
    onShowSettingsDialog: (Boolean) -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val error = state.errorMessage
        if (error != null) {
            val result = snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Retry",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onRetry()
            } else {
                onDismissError()
            }
        }
    }

    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact
    val maxContentWidth: Dp = remember(isCompact) { if (isCompact) Dp.Unspecified else 800.dp }
    val bubbleMaxWidthFraction = remember(isCompact) { if (isCompact) 0.85f else 0.70f }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = { onShowClearHistoryDialog(true) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear chat history",
                            )
                        }
                    }
                    IconButton(onClick = { onShowSettingsDialog(true) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
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
                    .fillMaxSize()
                    .widthIn(max = maxContentWidth)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    EmptyStatePlaceholder(modifier = Modifier.weight(1f))
                } else {
                    MessageList(
                        messages = state.messages,
                        isLoading = state.isLoading,
                        bubbleMaxWidthFraction = bubbleMaxWidthFraction,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    isLoading = state.isLoading,
                    isApiKeyConfigured = state.isApiKeyConfigured,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onShowSpeechError = onShowSpeechError,
                )
            }
        }
    }

    if (state.showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { onShowClearHistoryDialog(false) },
            title = { Text("Clear Chat History") },
            text = { Text("Are you sure you want to delete all messages? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirmClearHistory) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowClearHistoryDialog(false) }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (state.showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { onShowSettingsDialog(false) },
            title = { Text("Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Theme Mode",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (state.themeMode == mode),
                                    onClick = { onSetThemeMode(mode) },
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (state.themeMode == mode),
                                onClick = { onSetThemeMode(mode) },
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "System Default"
                                    ThemeMode.LIGHT -> "Light Theme"
                                    ThemeMode.DARK -> "Dark Theme"
                                },
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dynamic Color",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Use wallpaper colors (Android 12+)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.dynamicColor,
                            onCheckedChange = onSetDynamicColor,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onShowSettingsDialog(false) }) {
                    Text("Done")
                }
            },
        )
    }
}

@Composable
private fun EmptyStatePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.response_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    bubbleMaxWidthFraction: Float,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = messages,
            key = { message -> message.id },
        ) { message ->
            ChatMessageBubble(
                message = message,
                maxWidthFraction = bubbleMaxWidthFraction,
            )
        }

        if (isLoading) {
            item(key = "loading_indicator") {
                LoadingBubble(maxWidthFraction = bubbleMaxWidthFraction)
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    maxWidthFraction: Float = 0.85f,
    modifier: Modifier = Modifier,
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val containerColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val bubbleShape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            if (!isUser) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 6.dp, bottom = 4.dp)
                        .size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Surface(
                shape = bubbleShape,
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth(maxWidthFraction),
            ) {
                Text(
                    text = message.text.toBoldAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble(
    maxWidthFraction: Float = 0.85f,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_assistant),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(28.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.fillMaxWidth(maxWidthFraction),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini is thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    isLoading: Boolean,
    isApiKeyConfigured: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onShowSpeechError: (String) -> Unit,
) {
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                onPromptChange(spokenText)
            }
        }
    }

    val isInputEnabled = remember(isLoading, isApiKeyConfigured) { !isLoading && isApiKeyConfigured }
    val isSendEnabled = remember(isLoading, prompt, isApiKeyConfigured) { !isLoading && prompt.isNotBlank() && isApiKeyConfigured }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = 1,
            maxLines = 4,
            enabled = isInputEnabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )

        IconButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt...")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    onShowSpeechError("Speech recognition is not available on this device.")
                }
            },
            enabled = isInputEnabled,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Voice input",
                tint = if (isInputEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
            )
        }

        FilledIconButton(
            onClick = onSend,
            enabled = isSendEnabled,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                )
            }
        }
    }
}

// --- Previews ---

@Preview(name = "User Bubble", showBackground = true)
@Composable
private fun UserBubblePreview() {
    GeminiApiComposeStarterTheme {
        ChatMessageBubble(
            message = ChatMessage(text = "Hello! Explain Jetpack Compose in simple terms.", isUser = true),
        )
    }
}

@Preview(name = "Gemini Bubble", showBackground = true)
@Composable
private fun GeminiBubblePreview() {
    GeminiApiComposeStarterTheme {
        ChatMessageBubble(
            message = ChatMessage(
                text = "Jetpack Compose is Android's modern toolkit for building native UI using **Kotlin**.",
                isUser = false,
            ),
        )
    }
}

@Preview(name = "Chat Screen Light", showBackground = true)
@Composable
private fun ChatScreenLightPreview() {
    val sampleState = ChatUiState(
        messages = listOf(
            ChatMessage(text = "Hi Gemini, what is Compose?", isUser = true),
            ChatMessage(text = "Jetpack Compose simplifies UI development with **declarative** code.", isUser = false),
        ),
        prompt = "",
        isLoading = false,
    )
    GeminiApiComposeStarterTheme(darkTheme = false) {
        ChatScreenPreviewContent(sampleState)
    }
}

@Preview(name = "Chat Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ChatScreenDarkPreview() {
    val sampleState = ChatUiState(
        messages = listOf(
            ChatMessage(text = "Hi Gemini, what is Compose?", isUser = true),
            ChatMessage(text = "Jetpack Compose simplifies UI development with **declarative** code.", isUser = false),
        ),
        prompt = "",
        isLoading = false,
    )
    GeminiApiComposeStarterTheme(darkTheme = true) {
        ChatScreenPreviewContent(sampleState)
    }
}

@Preview(name = "Chat Screen Loading", showBackground = true)
@Composable
private fun ChatScreenLoadingPreview() {
    val sampleState = ChatUiState(
        messages = listOf(
            ChatMessage(text = "Explain Quantum Computing.", isUser = true),
        ),
        prompt = "",
        isLoading = true,
    )
    GeminiApiComposeStarterTheme {
        ChatScreenPreviewContent(sampleState)
    }
}

@Preview(name = "Chat Screen Error", showBackground = true)
@Composable
private fun ChatScreenErrorPreview() {
    val sampleState = ChatUiState(
        messages = listOf(
            ChatMessage(text = "What is the capital of France?", isUser = true),
        ),
        prompt = "What is the capital of France?",
        isLoading = false,
        errorMessage = "Network connection failed. Please try again.",
    )
    GeminiApiComposeStarterTheme {
        ChatScreenPreviewContent(sampleState)
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun ChatScreenPreviewContent(state: ChatUiState) {
    val dummyWindowSizeClass = WindowSizeClass.calculateFromSize(
        size = DpSize(360.dp, 800.dp),
    )
    ChatScreen(
        state = state,
        windowSizeClass = dummyWindowSizeClass,
        onPromptChange = {},
        onSend = {},
        onDismissError = {},
        onRetry = {},
        onShowSpeechError = {},
        onShowClearHistoryDialog = {},
        onConfirmClearHistory = {},
        onShowSettingsDialog = {},
        onSetThemeMode = {},
        onSetDynamicColor = {},
    )
}
