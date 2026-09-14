package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass,
    userPreferences: com.fahim.geminiApiComposeStarter.data.UserPreferences,
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
    windowSizeClass: androidx.compose.material3.windowsizeclass.WindowSizeClass? = null,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val listState = rememberLazyListState()
    val bubbleMaxWidth = when (
        windowSizeClass?.widthSizeClass
    ) {
        androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Expanded -> 520.dp
        androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> 400.dp
        else -> 320.dp
    }
    val speechLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->

            val spokenText =
                result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()

            if (spokenText != null) {
                onPromptChange(spokenText)
            }
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                val intent = Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                }

                speechLauncher.launch(intent)
            }
        }
    val startVoiceInput = {
        permissionLauncher.launch(
            android.Manifest.permission.RECORD_AUDIO
        )
    }



    /*
     * Show errors using Snackbar.
     */
    LaunchedEffect(state.errorMessage) {

        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    /*
     * Automatically scroll to the newest message.
     */
    LaunchedEffect(state.messages.size) {

        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                state.messages.lastIndex
            )
        }
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
                .padding(horizontal = 16.dp)
        ) {

            /*
             * Conversation
             */
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {

                items(
                    items = state.messages,
                    key = { message ->
                        message.id
                    },
                ) { message ->

                    ChatBubble(
                        message = message,
                        maxWidth = bubbleMaxWidth,
                    )
                }

                if (state.isLoading) {

                    item {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                            )

                            Text(
                                text = "Gemini is thinking..."
                            )
                        }
                    }
                }
            }

            /*
             * Input area
             */
            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onVoiceInput = startVoiceInput,
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    maxWidth: androidx.compose.ui.unit.Dp,
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (message.isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            }
    ) {

        Surface(
            modifier = Modifier
                .widthIn(max = maxWidth),

            shape = MaterialTheme.shapes.medium,

            color =
                if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
        ) {

            Column(
                modifier = Modifier.padding(12.dp)
            ) {

                Text(
                    text =
                        if (message.isUser) {
                            "You"
                        } else {
                            "Gemini"
                        },

                    fontWeight = FontWeight.Bold,

                    color =
                        if (message.isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )

                Text(
                    text = message.text,
                    modifier = Modifier.padding(top = 4.dp),
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
            .padding(vertical = 16.dp),

        verticalAlignment = Alignment.CenterVertically,
    ) {

        OutlinedTextField(
            value = prompt,

            onValueChange = onPromptChange,

            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),

            label = {
                Text("Enter your prompt here")
            },

            minLines = 3,

            enabled = enabled,

            isError = promptError != null,

            supportingText =
                if (promptError != null) {
                    {
                        Text("Field cannot be empty")
                    }
                } else {
                    null
                },
        )

        FilledIconButton(
            onClick = onVoiceInput,
            enabled = enabled,
        ) {
            Text("🎤")
        }

        FilledIconButton(
            onClick = onSend,
            enabled = enabled,
        ) {

            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
            )
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
                    ChatMessage(
                        id = 1,
                        text = "Hello! What is Kotlin?",
                        isUser = true,
                    ),
                    ChatMessage(
                        id = 2,
                        text = "Kotlin is a modern programming language.",
                        isUser = false,
                    ),
                )
            ),

            onPromptChange = {},
            onSend = {},
        )
    }
}