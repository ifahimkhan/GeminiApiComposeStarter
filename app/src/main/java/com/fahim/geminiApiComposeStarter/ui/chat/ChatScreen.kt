@file:OptIn(
    androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi::class
)

package com.fahim.geminiApiComposeStarter.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    windowSizeClass: WindowSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val listState = rememberLazyListState()

    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->

            val spokenText = result.data
                ?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )
                ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {
                onPromptChange(spokenText)
            }
        }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(state.messages.size, state.isLoading) {
        val itemCount = state.messages.size

        if (itemCount > 0) {
            listState.animateScrollToItem(itemCount - 1)
        }
    }

    val isExpanded =
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

    val contentMaxWidth = if (isExpanded) {
        900.dp
    } else {
        700.dp
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = contentMaxWidth)
        ) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isExpanded) {
                            24.dp
                        } else {
                            12.dp
                        }
                    ),
                contentPadding = PaddingValues(
                    vertical = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                items(
                    items = state.messages,
                    key = { message -> message.id },
                ) { message ->

                    MessageBubble(
                        message = message,
                        isExpanded = isExpanded,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.isLoading) {
                    item(key = "loading") {
                        LoadingBubble()
                    }
                }
            }

            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                isExpanded = isExpanded,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceInput = {
                    val intent = Intent(
                        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                    ).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )

                        putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "Speak your message"
                        )
                    }

                    speechRecognizerLauncher.launch(intent)
                },
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (message.isUser) {
            Arrangement.End
        } else {
            Arrangement.Start
        },
        verticalAlignment = Alignment.Top,
    ) {

        if (!message.isUser) {
            Icon(
                imageVector = Icons.Filled.Face,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 6.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(
                max = if (isExpanded) {
                    720.dp
                } else {
                    600.dp
                }
            ),
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp,
                ),
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            imageVector = Icons.Filled.Face,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        CircularProgressIndicator(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(24.dp),
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    isExpanded: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isExpanded) 24.dp else 12.dp,
                end = if (isExpanded) 24.dp else 12.dp,
                top = 8.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            label = {
                Text("Message Gemini")
            },
            minLines = 1,
            maxLines = 4,
            enabled = enabled,
            isError = promptError != null,
            supportingText = if (promptError != null) {
                {
                    Text("Message cannot be empty")
                }
            } else {
                null
            },
        )

        FilledIconButton(
            onClick = onVoiceInput,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .padding(top = 4.dp),
        ) {
            Text(
                text = "🎤",
                style = MaterialTheme.typography.titleMedium,
            )
        }

        FilledIconButton(
            onClick = onSend,
            enabled = enabled && prompt.isNotBlank(),
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
            )
        }
    }
}

@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = 1,
                        text = "Hello!",
                        isUser = true,
                    ),
                    ChatMessage(
                        id = 2,
                        text = "Hi! How can I help you?",
                        isUser = false,
                    ),
                ),
            ),
            windowSizeClass = WindowSizeClass.calculateFromSize(
                androidx.compose.ui.unit.DpSize(
                    400.dp,
                    800.dp,
                )
            ),
            onPromptChange = {},
            onSend = {},
        )
    }
}