package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.ChatEntity
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        windowSizeClass = windowSizeClass,
        onSend = viewModel::onSend,
        onClear = viewModel::clearHistory,
        onErrorDismissed = viewModel::onErrorDismissed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    windowSizeClass: WindowSizeClass,
    onSend: (String) -> Unit,
    onClear: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var prompt by remember { mutableStateOf("") }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            data?.firstOrNull()?.let { prompt = it }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_chat))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PromptBar(
                prompt = prompt,
                onPromptChange = { prompt = it },
                onSend = {
                    onSend(it)
                    prompt = ""
                },
                onVoiceInput = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    }
                    voiceLauncher.launch(intent)
                },
                isLoading = state.isLoading
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        windowWidthSizeClass = windowSizeClass.widthSizeClass
                    )
                }
                if (state.isLoading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatEntity,
    windowWidthSizeClass: WindowWidthSizeClass
) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
    
    val maxWidthPercent = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> 0.8f
        WindowWidthSizeClass.Medium -> 0.7f
        else -> 0.6f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(maxWidthPercent)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = if (isUser) stringResource(R.string.user_label) else stringResource(R.string.gemini_label),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content.toBoldAnnotatedString(),
                    color = textColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PromptBar(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onVoiceInput: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.imePadding()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
                maxLines = 4,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onVoiceInput, enabled = !isLoading) {
                Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.voice_input))
            }
            IconButton(
                onClick = { onSend(prompt) },
                enabled = !isLoading && prompt.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatEntity(id = 1, role = "user", content = "Hello!"),
                    ChatEntity(id = 2, role = "model", content = "Hi there! I am **Gemini**.")
                )
            ),
            windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp)),
            onSend = {},
            onClear = {},
            onErrorDismissed = {}
        )
    }
}
