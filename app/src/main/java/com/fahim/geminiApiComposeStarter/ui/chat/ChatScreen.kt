@file:OptIn(ExperimentalMaterial3Api::class)

package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.UserSettings
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiTheme
import java.util.Locale

object ChatTestTags {
    const val MESSAGE_LIST = "message_list"
    const val EMPTY_STATE = "empty_state"
    const val INPUT = "chat_input"
    const val SEND = "send_button"
    const val MIC = "mic_button"
    const val LOADING = "loading_indicator"
    const val ERROR = "error_card"
    const val RETRY = "retry_button"
    const val SETTINGS_BUTTON = "settings_button"
    const val SETTINGS_PANE = "settings_pane"
}

/**
 * Stateless chat screen: everything comes in through [uiState], every event goes out
 * through a callback. (Only UI-mechanics objects like the list scroll position live here.)
 */
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onClearHistory: () -> Unit,
    onSettingsOpenChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded = widthSizeClass == WindowWidthSizeClass.Expanded
    val isCompact = widthSizeClass == WindowWidthSizeClass.Compact

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Gemini Chat") },
                actions = {
                    if (!isExpanded) {
                        IconButton(
                            onClick = { onSettingsOpenChange(true) },
                            modifier = Modifier.testTag(ChatTestTags.SETTINGS_BUTTON),
                        ) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
                    }
                },
            )
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            // Tablets / landscape on large screens: permanent settings pane.
            if (isExpanded) {
                Surface(
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .testTag(ChatTestTags.SETTINGS_PANE),
                ) {
                    SettingsContent(
                        settings = uiState.settings,
                        hasMessages = uiState.messages.isNotEmpty(),
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                        onClearHistory = onClearHistory,
                    )
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Keep the conversation a readable width on big screens.
                Column(
                    Modifier
                        .widthIn(max = 840.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    MessageList(
                        messages = uiState.messages,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        canRetry = uiState.failedPrompt != null,
                        bubbleWidthFraction = if (isCompact) 0.82f else 0.65f,
                        onRetry = onRetry,
                        onDismissError = onDismissError,
                        modifier = Modifier.weight(1f),
                    )
                    ChatInputBar(
                        input = uiState.input,
                        canSend = uiState.canSend,
                        isLoading = uiState.isLoading,
                        onInputChange = onInputChange,
                        onSend = onSend,
                        onVoiceResult = onVoiceResult,
                        onVoiceUnavailable = onVoiceUnavailable,
                    )
                }
            }
        }
    }

    if (uiState.isSettingsOpen && !isExpanded) {
        ModalBottomSheet(onDismissRequest = { onSettingsOpenChange(false) }) {
            SettingsContent(
                settings = uiState.settings,
                hasMessages = uiState.messages.isNotEmpty(),
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onClearHistory = onClearHistory,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    errorMessage: String?,
    canRetry: Boolean,
    bubbleWidthFraction: Float,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val itemCount = messages.size + (if (isLoading) 1 else 0) + (if (errorMessage != null) 1 else 0)

    // Auto-scroll to the newest item whenever something is added.
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maxBubbleWidth = maxWidth * bubbleWidthFraction

        if (itemCount == 0) {
            EmptyState(Modifier.align(Alignment.Center))
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag(ChatTestTags.MESSAGE_LIST),
        ) {
            items(
                items = messages,
                key = { it.id }, // stable keys: items are not re-created when the list grows
                contentType = { if (it.isFromUser) "user" else "model" },
            ) { message ->
                MessageBubble(message, maxBubbleWidth, Modifier.animateItem())
            }
            if (isLoading) {
                item(key = "loading", contentType = "loading") {
                    TypingBubble(Modifier.animateItem())
                }
            }
            if (errorMessage != null) {
                item(key = "error", contentType = "error") {
                    ErrorCard(errorMessage, canRetry, onRetry, onDismissError, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(32.dp)
            .testTag(ChatTestTags.EMPTY_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Ask Gemini anything", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.size(8.dp))
        Text(
            "Type a message or tap the mic to speak.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val UserBubbleShape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
private val ModelBubbleShape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)

@Composable
private fun MessageBubble(message: ChatMessage, maxWidth: Dp, modifier: Modifier = Modifier) {
    val isUser = message.isFromUser
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = if (isUser) UserBubbleShape else ModelBubbleShape,
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = if (isUser) "You" else "Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary,
                )
                val displayText = remember(message.text, isUser) {
                    if (isUser) AnnotatedString(message.text) else message.text.toBoldAnnotatedString()
                }
                SelectionContainer {
                    Text(displayText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun TypingBubble(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(shape = ModelBubbleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(18.dp)
                        .testTag(ChatTestTags.LOADING),
                )
                Spacer(Modifier.width(10.dp))
                Text("Gemini is thinking…", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(ChatTestTags.ERROR),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.size(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                if (canRetry) {
                    Button(onClick = onRetry, modifier = Modifier.testTag(ChatTestTags.RETRY)) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    canSend: Boolean,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
) {
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

    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = !isLoading,
                modifier = Modifier.testTag(ChatTestTags.MIC),
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Gemini…")
                    }
                    try {
                        voiceLauncher.launch(intent)
                    } catch (e: ActivityNotFoundException) {
                        onVoiceUnavailable()
                    }
                },
            ) { Icon(painterResource(R.drawable.ic_mic), contentDescription = "Voice input") }

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Message Gemini") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                modifier = Modifier
                    .weight(1f)
                    .testTag(ChatTestTags.INPUT),
            )

            Spacer(Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.testTag(ChatTestTags.SEND),
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
        }
    }
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

@Composable
private fun SettingsContent(
    settings: UserSettings,
    hasMessages: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)

        Text("Theme", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = settings.themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                ) { Text(mode.label) }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dynamic colour", Modifier.weight(1f))
                Switch(checked = settings.dynamicColor, onCheckedChange = onDynamicColorChange)
            }
        }

        HorizontalDivider()

        OutlinedButton(onClick = onClearHistory, enabled = hasMessages) {
            Icon(Icons.Filled.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Clear chat history")
        }
    }
}

// ---------- Previews ----------

private val previewMessages = listOf(
    ChatMessage(1, "What is Jetpack Compose?", isFromUser = true, timestamp = 1),
    ChatMessage(2, "Jetpack Compose is Android's modern declarative UI toolkit.", isFromUser = false, timestamp = 2),
)

@Preview(showBackground = true)
@Composable
private fun ChatScreenPhonePreview() {
    GeminiTheme(dynamicColor = false) {
        ChatScreen(
            uiState = ChatUiState(messages = previewMessages, isLoading = true),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onInputChange = {}, onSend = {}, onVoiceResult = {}, onVoiceUnavailable = {},
            onRetry = {}, onDismissError = {}, onClearHistory = {}, onSettingsOpenChange = {},
            onThemeModeChange = {}, onDynamicColorChange = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1100, heightDp = 700)
@Composable
private fun ChatScreenTabletPreview() {
    GeminiTheme(dynamicColor = false) {
        ChatScreen(
            uiState = ChatUiState(
                messages = previewMessages,
                errorMessage = "Network error. Check your connection and try again.",
                failedPrompt = "Tell me more",
            ),
            widthSizeClass = WindowWidthSizeClass.Expanded,
            onInputChange = {}, onSend = {}, onVoiceResult = {}, onVoiceUnavailable = {},
            onRetry = {}, onDismissError = {}, onClearHistory = {}, onSettingsOpenChange = {},
            onThemeModeChange = {}, onDynamicColorChange = {},
        )
    }
}
