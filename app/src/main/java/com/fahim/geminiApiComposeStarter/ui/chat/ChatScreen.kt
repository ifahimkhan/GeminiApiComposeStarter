package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onClearChat = viewModel::clearChatHistory,
        onToggleTheme = viewModel::toggleTheme,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Speech Recognizer Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText != null) {
                onPromptChange(spokenText)
            }
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.microphone))
            }
            speechLauncher.launch(intent)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.permission_denied))
            }
        }
    }

    // Show errors in a Snackbar.
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // Scroll to the newest message.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                index = state.messages.lastIndex,
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (state.isDarkMode == true) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.theme_toggle)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClearChat) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.clear_chat)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {

            // Conversation area.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = 12.dp,
                    ),
                ) {
                    if (state.messages.isEmpty()) {
                        item(key = "welcome_message") {
                            AssistantBubble(
                                text = "Hello! How can I help you today?",
                            )
                        }
                    }

                    items(
                        items = state.messages,
                        key = { message -> message.id },
                    ) { message ->

                        if (message.isFromUser) {
                            UserBubble(text = message.text)
                        } else {
                            AssistantBubble(text = message.text)
                        }
                    }
                }

                // Loading indicator while Gemini is thinking.
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .padding(all = 8.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,
                onMicClick = {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }
    }
}

@Composable
private fun UserBubble(
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun AssistantBubble(
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "🤖",
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 8.dp),
        )

        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = text.toBoldAnnotatedString(),
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
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
    onMicClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
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
            leadingIcon = {
                IconButton(onClick = onMicClick, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.microphone)
                    )
                }
            },
            minLines = 1,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                {
                    Text(stringResource(R.string.field_cannot_be_empty))
                }
            },
        )

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

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = 1,
                        text = "Hello! How can I help you today?",
                        isFromUser = false,
                    ),
                    ChatMessage(
                        id = 2,
                        text = "Explain artificial intelligence.",
                        isFromUser = true,
                    ),
                    ChatMessage(
                        id = 3,
                        text = "**AI** is technology that helps computers learn and solve problems.",
                        isFromUser = false,
                    ),
                ),
            ),
            onPromptChange = {},
            onSend = {},
            onClearChat = {},
            onToggleTheme = {},
        )
    }
}
