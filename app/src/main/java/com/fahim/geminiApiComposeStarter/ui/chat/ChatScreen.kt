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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Role
import com.fahim.geminiApiComposeStarter.data.model.ThemeMode
import com.fahim.geminiApiComposeStarter.data.model.UserPreferences
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

/** Readable line length for chat content on tablets and landscape. */
private val MaxContentWidth = 840.dp

/** Connects [ChatViewModel] to the stateless [ChatScreen] and owns the speech launcher. */
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    widthSizeClass: WindowWidthSizeClass,
    heightSizeClass: WindowHeightSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
    }
    val voiceAvailable = remember {
        speechIntent.resolveActivity(context.packageManager) != null
    }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(viewModel::onVoiceInput)
        }
    }

    ChatScreen(
        state = state,
        widthSizeClass = widthSizeClass,
        heightSizeClass = heightSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onMicClick = if (voiceAvailable) {
            {
                try {
                    speechLauncher.launch(speechIntent)
                } catch (e: ActivityNotFoundException) {
                    // Recogniser was uninstalled after the check; nothing to launch.
                }
            }
        } else {
            null
        },
        onErrorShown = viewModel::onErrorShown,
        onClearHistory = viewModel::onClearHistory,
        onThemeModeChange = viewModel::onThemeModeChange,
        onDynamicColorChange = viewModel::onDynamicColorChange,
    )
}

/**
 * Stateless chat screen. Everything it shows comes from [state]; every user action is
 * reported through a callback. [onMicClick] is null when no speech recogniser is installed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    widthSizeClass: WindowWidthSizeClass,
    heightSizeClass: WindowHeightSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: (() -> Unit)?,
    onErrorShown: () -> Unit,
    onClearHistory: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            // Clear it so the same error shows again if it happens twice in a row.
            onErrorShown()
        }
    }

    val isCompactWidth = widthSizeClass == WindowWidthSizeClass.Compact
    val isCompactHeight = heightSizeClass == WindowHeightSizeClass.Compact

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    SettingsMenu(
                        preferences = state.preferences,
                        canClear = state.messages.isNotEmpty() && !state.isLoading,
                        onClearHistory = onClearHistory,
                        onThemeModeChange = onThemeModeChange,
                        onDynamicColorChange = onDynamicColorChange,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .then(if (isCompactWidth) Modifier else Modifier.widthIn(max = MaxContentWidth))
                    .fillMaxSize(),
            ) {
                MessageList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    bubbleMaxWidthFraction = if (isCompactWidth) 0.85f else 0.7f,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    // In landscape on a phone vertical space is scarce.
                    minLines = if (isCompactHeight) 1 else 2,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onMicClick = onMicClick,
                )
            }
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
    if (messages.isEmpty() && !isLoading) {
        Box(modifier = modifier.padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.empty_chat),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val itemCount = messages.size + if (isLoading) 1 else 0
    // Keep the newest message (or the typing indicator) in view.
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("message_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = messages, key = { it.id }, contentType = { it.role }) { message ->
            MessageBubble(message = message, maxWidthFraction = bubbleMaxWidthFraction)
        }
        if (isLoading) {
            item(key = "typing", contentType = "typing") { TypingBubble() }
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    minLines: Int,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .testTag("prompt_input"),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = minLines,
            maxLines = 5,
            enabled = enabled,
            isError = promptError != null,
            supportingText = {
                if (promptError != null) {
                    Text(stringResource(R.string.field_cannot_be_empty))
                } else {
                    Text(
                        text = "${prompt.length} / ${ChatViewModel.MAX_PROMPT_LENGTH}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                    )
                }
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onMicClick != null) {
                IconButton(
                    onClick = onMicClick,
                    enabled = enabled,
                    modifier = Modifier.testTag("mic_button"),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = stringResource(R.string.voice_input),
                    )
                }
            }
            FilledIconButton(
                onClick = onSend,
                enabled = enabled,
                modifier = Modifier.testTag("send_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                )
            }
        }
    }
}

@Composable
private fun SettingsMenu(
    preferences: UserPreferences,
    canClear: Boolean,
    onClearHistory: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("settings_button")) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.settings))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ThemeMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(stringResource(mode.labelRes)) },
                leadingIcon = { RadioButton(selected = preferences.themeMode == mode, onClick = null) },
                onClick = { onThemeModeChange(mode) },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.dynamic_color)) },
            leadingIcon = { Checkbox(checked = preferences.dynamicColor, onCheckedChange = null) },
            onClick = { onDynamicColorChange(!preferences.dynamicColor) },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.clear_chat)) },
            enabled = canClear,
            onClick = {
                expanded = false
                onClearHistory()
            },
            modifier = Modifier.testTag("clear_chat"),
        )
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(1, Role.USER, "What is Jetpack Compose?", 0),
                    ChatMessage(2, Role.MODEL, "**Jetpack Compose** is Android's modern UI toolkit.", 1),
                ),
                isLoading = true,
            ),
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium,
            onPromptChange = {},
            onSend = {},
            onMicClick = {},
            onErrorShown = {},
            onClearHistory = {},
            onThemeModeChange = {},
            onDynamicColorChange = {},
        )
    }
}
