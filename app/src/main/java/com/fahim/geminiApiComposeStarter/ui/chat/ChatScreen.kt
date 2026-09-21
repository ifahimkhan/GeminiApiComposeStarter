package com.fahim.geminiApiComposeStarter.ui.chat

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.window.core.layout.WindowWidthSizeClass

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        darkMode = darkMode,
        onDarkModeChange = onDarkModeChange,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    /*
     * WindowSizeClass
     *
     * The UI automatically adapts to the available screen width.
     *
     * Compact  -> Phone
     * Medium   -> Tablet / small landscape
     * Expanded -> Large tablet / desktop
     */
    val windowSizeClass =
        currentWindowAdaptiveInfo().windowSizeClass

    val isCompact =
        windowSizeClass.windowWidthSizeClass ==
                WindowWidthSizeClass.COMPACT

    val isExpanded =
        windowSizeClass.windowWidthSizeClass ==
                WindowWidthSizeClass.EXPANDED

    /*
     * Speech-to-text launcher.
     */
    val speechLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == android.app.Activity.RESULT_OK) {

                val spokenText =
                    result.data
                        ?.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS
                        )
                        ?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    onPromptChange(spokenText)
                }
            }
        }

    fun startSpeechRecognition() {

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    java.util.Locale.getDefault()
                )

                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Speak your message"
                )
            }

        speechLauncher.launch(intent)
    }

    /*
     * Show error through Snackbar.
     */
    LaunchedEffect(state.errorMessage) {

        state.errorMessage?.let { message ->

            snackbarHostState.showSnackbar(message)
        }
    }

    /*
     * Automatically scroll to the latest message.
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
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = if (isCompact) {
                        16.dp
                    } else {
                        32.dp
                    },
                    vertical = 16.dp
                )
        ) {

            /*
             * Top bar
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Gemini Chat",

                    style =
                        if (isExpanded) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.headlineSmall
                        },

                    modifier =
                        Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        onDarkModeChange(!darkMode)
                    }
                ) {

                    Text(
                        text =
                            if (darkMode) {
                                "☀"
                            } else {
                                "☾"
                            },

                        fontSize = 24.sp
                    )
                }
            }

            /*
             * Chat messages
             *
             * On expanded screens the conversation
             * is intentionally kept narrower so that
             * messages remain easy to read.
             */
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),

                state = listState,

                verticalArrangement =
                    Arrangement.spacedBy(12.dp),

                horizontalAlignment =
                    if (isExpanded) {
                        Alignment.CenterHorizontally
                    } else {
                        Alignment.Start
                    }
            ) {

                items(
                    items = state.messages,

                    key = { message ->
                        message.id
                    }
                ) { message ->

                    ChatBubble(
                        message = message,
                        expanded = isExpanded
                    )
                }
            }

            /*
             * Loading indicator
             */
            if (state.isLoading) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),

                    horizontalArrangement =
                        Arrangement.Center
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(28.dp)
                    )
                }
            }

            /*
             * Prompt input
             */
            PromptBar(
                prompt = state.prompt,

                promptError =
                    state.promptError,

                enabled =
                    !state.isLoading,

                expanded =
                    isExpanded,

                onPromptChange =
                    onPromptChange,

                onSend =
                    onSend,

                onSpeechClick = {
                    startSpeechRecognition()
                }
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    expanded: Boolean
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth(
                    if (expanded) {
                        0.70f
                    } else {
                        1f
                    }
                ),

        horizontalArrangement =
            if (message.isUser) {
                Arrangement.End
            } else {
                Arrangement.Start
            },

        verticalAlignment =
            Alignment.Top
    ) {

        /*
         * Assistant icon
         */
        if (!message.isUser) {

            Icon(
                imageVector =
                    Icons.Default.Face,

                contentDescription =
                    "Assistant",

                modifier =
                    Modifier
                        .size(40.dp)
                        .padding(4.dp),

                tint =
                    MaterialTheme.colorScheme.primary
            )
        }

        /*
         * Chat bubble
         */
        Surface(
            shape =
                MaterialTheme.shapes.large,

            color =
                if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },

            modifier =
                Modifier.fillMaxWidth(
                    if (expanded) {
                        0.85f
                    } else {
                        0.85f
                    }
                )
        ) {

            Text(
                text = message.text,

                modifier =
                    Modifier.padding(14.dp),

                fontSize =
                    if (expanded) {
                        17.sp
                    } else {
                        16.sp
                    },

                color =
                    if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
            )
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    expanded: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onSpeechClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * Text field
         */
        OutlinedTextField(
            value = prompt,

            onValueChange =
                onPromptChange,

            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = 8.dp),

            label = {
                Text("Enter your prompt")
            },

            minLines =
                if (expanded) {
                    1
                } else {
                    2
                },

            enabled =
                enabled,

            isError =
                promptError != null,

            supportingText = {

                if (promptError != null) {

                    Text(
                        "Field cannot be empty"
                    )
                }
            }
        )

        /*
         * Speech-to-text button
         */
        OutlinedIconButton(
            onClick =
                onSpeechClick,

            enabled =
                enabled,

            modifier =
                Modifier.padding(end = 8.dp)
        ) {

            Text(
                text = "🎤",
                fontSize = 20.sp
            )
        }

        /*
         * Send button
         */
        FilledIconButton(
            onClick =
                onSend,

            enabled =
                enabled &&
                        prompt.isNotBlank()
        ) {

            Icon(
                imageVector =
                    Icons.AutoMirrored.Filled.Send,

                contentDescription =
                    "Send"
            )
        }
    }
}