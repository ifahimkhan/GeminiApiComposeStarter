@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.AppPreferences
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChatTags {
    const val MESSAGE_LIST = "message_list"
    const val EMPTY_STATE = "empty_state"
    const val SUGGESTION = "suggestion_chip"
    const val INPUT = "chat_input"
    const val SEND = "send_button"
    const val MIC = "mic_button"
    const val LOADING = "loading_indicator"
    const val ERROR = "error_banner"
    const val RETRY = "retry_button"
    const val SETTINGS_BUTTON = "settings_button"
    const val SETTINGS_PANE = "settings_pane"
    const val CLEAR = "clear_button"
}

private val Suggestions = listOf(
    "Explain Jetpack Compose in simple words",
    "Write a short poem about Android",
    "Give me 3 tips for clean Kotlin code",
)

/**
 * Stateless chat screen. Everything it shows comes from [uiState]; every user action is
 * reported through a callback. Only UI mechanics (scroll position) are remembered here.
 */
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit,
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

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gemini Assistant", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (uiState.isLoading) "typing…" else "online",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClearHistory,
                        enabled = uiState.messages.isNotEmpty() && !uiState.isLoading,
                        modifier = Modifier.testTag(ChatTags.CLEAR),
                    ) { Icon(Icons.Filled.Delete, contentDescription = "Clear chat") }
                },
                actions = {
                    if (!isExpanded) {
                        IconButton(
                            onClick = { onSettingsOpenChange(true) },
                            modifier = Modifier.testTag(ChatTags.SETTINGS_BUTTON),
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
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    ConversationList(
                        messages = uiState.messages,
                        isLoading = uiState.isLoading,
                        bubbleFraction = if (widthSizeClass == WindowWidthSizeClass.Compact) 0.85f else 0.6f,
                        onSuggestionClick = onSuggestionClick,
                        modifier = Modifier.weight(1f),
                    )
                    uiState.errorMessage?.let { message ->
                        ErrorBanner(
                            message = message,
                            canRetry = uiState.failedPrompt != null,
                            onRetry = onRetry,
                            onDismiss = onDismissError,
                        )
                    }
                    Composer(
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

            // Large screens: settings live in a permanent panel on the right.
            if (isExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .testTag(ChatTags.SETTINGS_PANE),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Appearance", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        AppearanceOptions(uiState.settings, onThemeModeChange, onDynamicColorChange)
                    }
                }
            }
        }
    }

    // Phones / small tablets: settings open in a dialog.
    if (uiState.isSettingsOpen && !isExpanded) {
        AlertDialog(
            onDismissRequest = { onSettingsOpenChange(false) },
            title = { Text("Appearance") },
            text = { AppearanceOptions(uiState.settings, onThemeModeChange, onDynamicColorChange) },
            confirmButton = {
                TextButton(onClick = { onSettingsOpenChange(false) }) { Text("Done") }
            },
        )
    }
}

@Composable
private fun ConversationList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    bubbleFraction: Float,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val count = messages.size + if (isLoading) 1 else 0

    LaunchedEffect(count) {
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maxBubble = maxWidth * bubbleFraction

        if (count == 0) {
            WelcomePanel(onSuggestionClick, Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(ChatTags.MESSAGE_LIST),
            ) {
                items(
                    items = messages,
                    key = { it.id },
                    contentType = { if (it.isFromUser) "mine" else "gemini" },
                ) { msg ->
                    MessageRow(msg, maxBubble, Modifier.animateItem())
                }
                if (isLoading) {
                    item(key = "typing", contentType = "typing") { TypingRow(Modifier.animateItem()) }
                }
            }
        }
    }
}

@Composable
private fun WelcomePanel(onSuggestionClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(24.dp)
            .testTag(ChatTags.EMPTY_STATE),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GeminiAvatar(size = 56.dp)
        Spacer(Modifier.height(16.dp))
        Text("How can I help today?", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick an idea below, type a message, or use the mic.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Suggestions.forEach { idea ->
                SuggestionChip(
                    onClick = { onSuggestionClick(idea) },
                    label = { Text(idea) },
                    modifier = Modifier.testTag(ChatTags.SUGGESTION),
                )
            }
        }
    }
}

@Composable
private fun GeminiAvatar(size: Dp = 32.dp) {
    Box(
        Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "G",
            color = MaterialTheme.colorScheme.onTertiary,
            fontWeight = FontWeight.Bold,
            style = if (size > 40.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
        )
    }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
private fun MessageRow(message: ChatMessage, maxBubble: Dp, modifier: Modifier = Modifier) {
    val mine = message.isFromUser
    val shown: AnnotatedString = remember(message.text, mine) {
        if (mine) AnnotatedString(message.text) else message.text.toBoldAnnotatedString()
    }
    val time = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            GeminiAvatar()
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = if (mine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.widthIn(max = maxBubble),
            ) {
                SelectionContainer {
                    Text(
                        shown,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            Text(
                text = time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
            )
        }
    }
}

@Composable
private fun TypingRow(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        GeminiAvatar()
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                "Gemini is writing…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .width(120.dp)
                    .testTag(ChatTags.LOADING),
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(ChatTags.ERROR),
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (canRetry) {
                TextButton(onClick = onRetry, modifier = Modifier.testTag(ChatTags.RETRY)) { Text("Retry") }
            }
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}

@Composable
private fun Composer(
    input: String,
    canSend: Boolean,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onVoiceUnavailable: () -> Unit,
) {
    val speechLauncher = rememberLauncherForActivityResult(
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
        Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Ask Gemini…") },
            maxLines = 5,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            trailingIcon = {
                IconButton(
                    enabled = !isLoading,
                    modifier = Modifier.testTag(ChatTags.MIC),
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your question")
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            onVoiceUnavailable()
                        }
                    },
                ) { Icon(painterResource(R.drawable.ic_mic), contentDescription = "Speak") }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
            modifier = Modifier
                .weight(1f)
                .testTag(ChatTags.INPUT),
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier
                .size(52.dp)
                .testTag(ChatTags.SEND),
        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send") }
    }
}

private val ThemeMode.title: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Follow system"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }

@Composable
private fun AppearanceOptions(
    prefs: AppPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    Column {
        ThemeMode.entries.forEach { mode ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = prefs.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        role = Role.RadioButton,
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = prefs.themeMode == mode, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(mode.title)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wallpaper colours", Modifier.weight(1f))
                Switch(checked = prefs.dynamicColor, onCheckedChange = onDynamicColorChange)
            }
        }
    }
}

// ---------- Previews ----------

private val sample = listOf(
    ChatMessage(1, "What is Jetpack Compose?", isFromUser = true, timestamp = 0),
    ChatMessage(2, "**Jetpack Compose** is Android's modern UI toolkit.", isFromUser = false, timestamp = 0),
)

@Preview(showBackground = true)
@Composable
private fun PhonePreview() {
    GeminiTheme(dynamicColor = false) {
        ChatScreen(
            uiState = ChatUiState(messages = sample, isLoading = true),
            widthSizeClass = WindowWidthSizeClass.Compact,
            onInputChange = {}, onSend = {}, onSuggestionClick = {}, onVoiceResult = {},
            onVoiceUnavailable = {}, onRetry = {}, onDismissError = {}, onClearHistory = {},
            onSettingsOpenChange = {}, onThemeModeChange = {}, onDynamicColorChange = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 1100, heightDp = 700)
@Composable
private fun TabletPreview() {
    GeminiTheme(dynamicColor = false) {
        ChatScreen(
            uiState = ChatUiState(errorMessage = "No connection.", failedPrompt = "Hi"),
            widthSizeClass = WindowWidthSizeClass.Expanded,
            onInputChange = {}, onSend = {}, onSuggestionClick = {}, onVoiceResult = {},
            onVoiceUnavailable = {}, onRetry = {}, onDismissError = {}, onClearHistory = {},
            onSettingsOpenChange = {}, onThemeModeChange = {}, onDynamicColorChange = {},
        )
    }
}
