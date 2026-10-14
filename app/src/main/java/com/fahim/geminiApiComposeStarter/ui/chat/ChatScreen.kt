package com.fahim.geminiApiComposeStarter.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme

@Composable
fun ChatRoute(viewModel: ChatViewModel = viewModel()) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit
) {

    /*
     * Voice input launcher.
     * The spoken text is placed directly into the prompt box.
     */
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->

        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()

        if (!spokenText.isNullOrBlank()) {
            onPromptChange(spokenText)
        }
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            /*
             * CHAT HISTORY
             */
            val listState = rememberLazyListState()

            LaunchedEffect(state.messages.size) {
                if (state.messages.isNotEmpty()) {
                    listState.animateScrollToItem(
                        state.messages.lastIndex
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                itemsIndexed(
                    items = state.messages,
                    key = { index, message ->
                        "${message.isUser}-$index-${message.text.hashCode()}"
                    }
                ) { _, message ->

                    Text(
                        text = if (message.isUser) {
                            "You: ${message.text}"
                        } else {
                            "🤖: ${message.text}"
                        },
                        fontSize = 16.sp,
                        fontWeight = if (message.isUser) {
                            FontWeight.Normal
                        } else {
                            FontWeight.Normal
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                /*
                 * Show loading indicator while Gemini is responding.
                 */
                if (state.isLoading) {

                    item {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )

                            Spacer(
                                modifier = Modifier.size(8.dp)
                            )

                            Text(
                                text = "Gemini is thinking..."
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            /*
             * INPUT AREA
             */
            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
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
                            "Speak your question"
                        )
                    }

                    voiceLauncher.launch(intent)
                }
            )
        }
    }
}


/*
 * Bottom prompt bar.
 *
 * IMPORTANT:
 * The microphone and send buttons are placed inside
 * a Column, so they can NEVER overlap.
 */
@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,

            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),

            label = {
                Text("Ask Gemini anything...")
            },

            minLines = 3,

            enabled = enabled,

            isError = promptError != null,

            supportingText = promptError?.let {
                {
                    Text("Field cannot be empty")
                }
            }
        )

        /*
         * BUTTONS ARE VERTICAL.
         */
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            /*
             * VOICE BUTTON
             */
            FilledIconButton(
                onClick = onVoiceInput,
                enabled = enabled
            ) {
                Text(
                    text = "🎤",
                    fontSize = 18.sp
                )
            }

            /*
             * SEND BUTTON
             */
            FilledIconButton(
                onClick = onSend,
                enabled = enabled
            ) {
                Text(
                    text = "➤",
                    fontSize = 18.sp
                )
            }
        }
    }
}


@Composable
private fun ChatScreenPreview() {

    GeminiApiComposeStarterTheme {

        ChatScreen(
            state = ChatUiState(
                response = "",
                prompt = "What is Android?",
                messages = emptyList()
            ),
            onPromptChange = {},
            onSend = {}
        )
    }
}