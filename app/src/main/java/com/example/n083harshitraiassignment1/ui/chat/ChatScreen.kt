package com.example.n083harshitraiassignment1.ui.chat

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.n083harshitraiassignment1.model.ChatMessage
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel
) {

    val uiState by viewModel.uiState
        .collectAsStateWithLifecycle()

    val listState =
        rememberLazyListState()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val coroutineScope =
        rememberCoroutineScope()

    val voiceLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) { result ->

            val spokenText =
                result.data
                    ?.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    )
                    ?.firstOrNull()

            if (!spokenText.isNullOrBlank()) {

                viewModel.updateInput(
                    spokenText
                )
            }
        }

    LaunchedEffect(
        uiState.messages.size
    ) {

        if (uiState.messages.isNotEmpty()) {

            listState.animateScrollToItem(
                uiState.messages.lastIndex
            )
        }
    }

    LaunchedEffect(
        uiState.errorMessage
    ) {

        uiState.errorMessage?.let {

            snackbarHostState.showSnackbar(
                it
            )

            viewModel.clearError()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Gemini Assistant")
                },

                actions = {

                    IconButton(
                        onClick = {
                            viewModel.clearHistory()
                        }
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Delete,
                            contentDescription =
                                "Clear history"
                        )
                    }
                }
            )
        },

        snackbarHost = {

            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        },

        bottomBar = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                val inputModifier = if (maxWidth > 650.dp) {
                    Modifier.widthIn(max = 650.dp)
                } else {
                    Modifier.fillMaxWidth()
                }

                InputBar(
                    text = uiState.inputText,
                    onTextChange = {
                        viewModel.updateInput(it)
                    },
                    onSend = {
                        viewModel.sendMessage()
                    },
                    onVoice = {
                        try {
                            val intent =
                                Intent(
                                    RecognizerIntent
                                        .ACTION_RECOGNIZE_SPEECH
                                ).apply {
                                    putExtra(
                                        RecognizerIntent
                                            .EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent
                                            .LANGUAGE_MODEL_FREE_FORM
                                    )
                                    putExtra(
                                        RecognizerIntent
                                            .EXTRA_PROMPT,
                                        "Speak your question"
                                    )
                                }
                            voiceLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    "Voice input is not supported on this device/emulator."
                                )
                            }
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = inputModifier
                )
            }
        }
    ) { paddingValues ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentModifier = if (maxWidth > 650.dp) {
                Modifier.widthIn(max = 650.dp).fillMaxSize()
            } else {
                Modifier.fillMaxSize()
            }

            LazyColumn(
                state = listState,
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = uiState.messages,
                    key = { message -> message.id }
                ) { message ->
                    ChatBubble(
                        message = message
                    )
                }

                if (uiState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )
                            Text(
                                "Gemini is thinking..."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Ask Gemini...")
            },
            enabled = enabled,
            maxLines = 4
        )

        IconButton(
            onClick = onVoice,
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice input"
            )
        }

        IconButton(
            onClick = onSend,
            enabled = enabled && text.isNotBlank()
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send message"
            )
        }
    }
}