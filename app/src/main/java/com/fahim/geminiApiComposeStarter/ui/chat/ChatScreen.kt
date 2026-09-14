package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    val voiceLauncher = rememberLauncherForActivityResult(
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

    fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No speech recognizer app found on this device", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                MessageList(
                    messages = state.messages,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceInput = ::launchVoiceInput,
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 88.dp)
                        .testTag("loading_indicator"),
                )
            }
        }
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    if (messages.isEmpty()) {
        Box(modifier = modifier) {
            Text(
                text = stringResource(R.string.response_placeholder),
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center).padding(8.dp),
            )
        }
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("message_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Row(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            if (!message.isFromUser) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = message.text.toBoldAnnotatedString(),
                fontSize = 16.sp,
                modifier = Modifier.padding(start = if (message.isFromUser) 0.dp else 8.dp),
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
    onVoiceInput: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f).padding(end = 8.dp).testTag("prompt_field"),
            label = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = 3,
            enabled = enabled,
            isError = promptError != null,
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )
        IconButton(
            onClick = onVoiceInput,
            enabled = enabled,
            modifier = Modifier.testTag("voice_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Voice input",
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

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(id = "1", text = "Hello Gemini", isFromUser = true),
                    ChatMessage(id = "2", text = "**Hello** from Gemini.", isFromUser = false),
                ),
            ),
            onPromptChange = {},
            onSend = {},
        )
    }
}