package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ThemePreference
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import java.util.Locale
import androidx.compose.ui.platform.testTag
@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val spokenText = result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()
                    ?.trim()

                if (!spokenText.isNullOrEmpty()) {
                    viewModel.onPromptChange(spokenText)
                } else {
                    viewModel.onVoiceError(
                        "No speech recognised. Please try again."
                    )
                }
            }

            Activity.RESULT_CANCELED -> {
                // Preserve existing input when recognition is cancelled.
            }

            else -> {
                viewModel.onVoiceError(
                    "Speech recognition failed. Please try again."
                )
            }
        }
    }

    ChatScreen(
        state = state,
        windowWidthSizeClass = windowWidthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onErrorShown = viewModel::onErrorShown,
        onThemeChange = viewModel::onThemeChange,
        onVoiceInput = {
            val intent = Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    Locale.getDefault().toLanguageTag()
                )
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your message"
                )
            }

            try {
                voiceLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                viewModel.onVoiceError(
                    "Speech recognition is not available on this device."
                )
            } catch (e: SecurityException) {
                viewModel.onVoiceError(
                    "Speech recognition could not open. " +
                            "Check the speech app's permissions."
                )
            }
        }
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onErrorShown: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onVoiceInput: () -> Unit,
) {
    val horizontalPadding = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 48.dp
        else -> 96.dp
    }

    val bubbleMaxWidth = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 320.dp
        WindowWidthSizeClass.Medium -> 420.dp
        else -> 560.dp
    }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        val itemCount = state.messages.size +
                if (state.isLoading) 1 else 0

        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = horizontalPadding)
        ) {
            Text(
                text = "Gemini Chat",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreference.entries.forEach { theme ->
                    FilterChip(
                        selected = state.themePreference == theme,
                        onClick = { onThemeChange(theme) },
                        label = {
                            Text(
                                text = when (theme) {
                                    ThemePreference.SYSTEM -> "System"
                                    ThemePreference.LIGHT -> "Light"
                                    ThemePreference.DARK -> "Dark"
                                }
                            )
                        }
                    )
                }
            }

            if (state.messages.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ask a question to start chatting.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { message -> message.id }
                    ) { message ->
                        ChatBubble(
                            message = message,
                            maxWidth = bubbleMaxWidth
                        )
                    }

                    if (state.isLoading) {
                        item(key = "loading_indicator") {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )

                                Text(
                                    text = if (state.messages.isEmpty()) {
                                        "Loading chat history…"
                                    } else {
                                        "Gemini is thinking…"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceInput = onVoiceInput
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    maxWidth: Dp,
) {
    val backgroundColor = if (message.isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (message.isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        }
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxWidth),
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            contentColor = textColor
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (message.isUser) "You" else "Gemini",
                    style = MaterialTheme.typography.labelMedium
                )

                Text(
                    text = message.text.toBoldAnnotatedString(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
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
    onVoiceInput: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .testTag("message_input"),
            placeholder = { Text("Type a message…") },
            enabled = enabled,
            minLines = 1,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            isError = promptError != null,
            supportingText = if (promptError != null) {
                { Text("Please enter a message.") }
            } else {
                null
            }
        )

        IconButton(
            onClick = onVoiceInput,
            enabled = enabled
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Voice input"
            )
        }

        FilledIconButton(
            onClick = onSend,
            enabled = enabled && prompt.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message"
            )
        }
    }
}