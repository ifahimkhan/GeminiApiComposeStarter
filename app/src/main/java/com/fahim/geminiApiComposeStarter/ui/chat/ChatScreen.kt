package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.Participant
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    onVoiceInputClick: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val speechNotAvailableMessage = stringResource(R.string.speech_not_available)
    val speechPrompt = stringResource(R.string.speech_prompt)

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onVoiceInputResult(spokenText)
            }
        }
    }

    val defaultVoiceInputClick = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
        }
        try {
            speechRecognitionLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.onVoiceRecognitionError(speechNotAvailableMessage)
        } catch (e: Exception) {
            viewModel.onVoiceRecognitionError(speechNotAvailableMessage)
        }
    }

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onToggleTheme = viewModel::toggleTheme,
        onVoiceInputClick = onVoiceInputClick ?: defaultVoiceInputClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleTheme: () -> Unit = {},
    onVoiceInputClick: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        val totalItems = state.messages.size + if (state.isLoading) 1 else 0
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button"),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isDarkTheme) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
                            ),
                            contentDescription = if (state.isDarkTheme) {
                                stringResource(R.string.switch_to_light_mode)
                            } else {
                                stringResource(R.string.switch_to_dark_mode)
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isWideScreen = maxWidth >= 600.dp
            val isCompactHeight = maxHeight < 480.dp
            val contentHorizontalPadding = if (isWideScreen) 24.dp else 16.dp
            val bubbleMaxWidth = if (isWideScreen) 520.dp else 320.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.messages.isEmpty() && !state.isLoading) {
                    EmptyChatPlaceholder(
                        modifier = Modifier.weight(1f),
                        isCompactHeight = isCompactHeight,
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("chat_messages_list"),
                        contentPadding = PaddingValues(
                            horizontal = contentHorizontalPadding,
                            vertical = if (isCompactHeight) 8.dp else 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = state.messages,
                            key = { it.id },
                        ) { message ->
                            ChatMessageItem(
                                message = message,
                                maxBubbleWidth = bubbleMaxWidth,
                            )
                        }

                        if (state.isLoading) {
                            item(key = "loading_indicator_bubble") {
                                LoadingMessageItem()
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
                    onVoiceInputClick = onVoiceInputClick,
                    isCompactHeight = isCompactHeight,
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    maxBubbleWidth: Dp = 320.dp,
) {
    val isUser = message.participant == Participant.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Gemini",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Surface(
            shape = if (isUser) {
                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
            },
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.widthIn(max = maxBubbleWidth, min = 40.dp),
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

@Composable
private fun LoadingMessageItem() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Gemini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Surface(
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini is thinking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder(
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(if (isCompactHeight) 16.dp else 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_assistant),
                contentDescription = null,
                modifier = Modifier.size(if (isCompactHeight) 36.dp else 56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = if (isCompactHeight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 8.dp))
            Text(
                text = stringResource(R.string.response_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    onVoiceInputClick: () -> Unit,
    isCompactHeight: Boolean = false,
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = if (isCompactHeight) 4.dp else 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_input"),
                placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
                maxLines = if (isCompactHeight) 2 else 4,
                enabled = enabled,
                isError = promptError != null,
                supportingText = promptError?.let {
                    { Text(stringResource(R.string.field_cannot_be_empty)) }
                },
                shape = RoundedCornerShape(24.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onVoiceInputClick,
                enabled = enabled,
                modifier = Modifier.testTag("mic_button"),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.voice_input),
                    tint = MaterialTheme.colorScheme.primary,
                )
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

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(text = "Hello Gemini!", participant = Participant.USER),
                    ChatMessage(text = "**Hello!** How can I help you today?", participant = Participant.MODEL),
                ),
            ),
            onPromptChange = {},
            onSend = {},
        )
    }
}
