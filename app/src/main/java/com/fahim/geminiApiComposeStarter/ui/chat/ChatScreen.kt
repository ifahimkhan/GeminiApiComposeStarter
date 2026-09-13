package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowSizeClass: WindowSizeClass,
    darkMode: Boolean,
    dynamicColors: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        state = state,
        windowSizeClass = windowSizeClass,
        darkMode = darkMode,
        dynamicColors = dynamicColors,
        onDarkModeChange = onDarkModeChange,
        onDynamicColorsChange = onDynamicColorsChange,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    windowSizeClass: WindowSizeClass? = null,
    darkMode: Boolean = false,
    dynamicColors: Boolean = true,
    onDarkModeChange: (Boolean) -> Unit = {},
    onDynamicColorsChange: (Boolean) -> Unit = {},
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val isLargeScreen =
        windowSizeClass?.widthSizeClass != WindowWidthSizeClass.Compact
    val coroutineScope = rememberCoroutineScope()

    // Speech recognition launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
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

    // Microphone permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->

        if (granted) {

            voiceLauncher.launch(
                createSpeechIntent()
            )

        } else {

            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    "Microphone permission is required for voice input."
                )
            }
        }
    }

    // Show errors
    LaunchedEffect(state.errorMessage) {

        state.errorMessage?.let { message ->

            snackbarHostState.showSnackbar(
                message
            )
        }
    }

    // Automatically scroll to newest message
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
            SnackbarHost(
                hostState = snackbarHostState
            )
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = if (isLargeScreen) 48.dp else 16.dp,
                    vertical = 16.dp
                )
        ) {

            // -------------------------
            // SETTINGS
            // -------------------------

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 2.dp,
            ) {

                Column(
                    modifier = Modifier.padding(12.dp)
                ) {

                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {

                        Text(
                            text = "Dark mode"
                        )

                        Switch(
                            checked = darkMode,
                            onCheckedChange = onDarkModeChange,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {

                        Text(
                            text = "Dynamic colors"
                        )

                        Switch(
                            checked = dynamicColors,
                            onCheckedChange = onDynamicColorsChange,
                        )
                    }
                }
            }

            // -------------------------
            // CHAT MESSAGES
            // -------------------------

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp),

                verticalArrangement = Arrangement.spacedBy(
                    8.dp
                ),
            ) {

                items(
                    items = state.messages,
                    key = { message ->
                        message.id
                    }
                ) { message ->

                    ChatBubble(
                        message = message
                    )
                }
            }

            // -------------------------
            // LOADING
            // -------------------------

            if (state.isLoading) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),

                    horizontalArrangement =
                        Arrangement.Center,
                ) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // -------------------------
            // PROMPT BAR
            // -------------------------

            PromptBar(
                prompt = state.prompt,
                promptError = state.promptError,
                enabled = !state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend,

                onVoiceInput = {

                    // Check speech recognition availability
                    if (!isSpeechRecognitionAvailable(context)) {

                        coroutineScope.launch {

                            snackbarHostState.showSnackbar(
                                "Speech recognition is not available on this device."
                            )
                        }

                    } else {

                        // Check microphone permission
                        val permissionGranted =
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                        if (permissionGranted) {

                            // Permission already granted
                            voiceLauncher.launch(
                                createSpeechIntent()
                            )

                        } else {

                            // Request microphone permission
                            permissionLauncher.launch(
                                Manifest.permission.RECORD_AUDIO
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
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
            modifier = Modifier.fillMaxWidth(0.85f),

            shape = MaterialTheme.shapes.medium,

            color =
                if (message.isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
        ) {

            Text(
                text = message.text,

                modifier = Modifier.padding(12.dp),

                color =
                    if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),

        verticalAlignment =
            Alignment.CenterVertically,
    ) {

        OutlinedTextField(
            value = prompt,

            onValueChange =
                onPromptChange,

            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),

            label = {

                Text(
                    stringResource(
                        R.string.enter_your_prompt_here
                    )
                )
            },

            minLines = 3,

            enabled = enabled,

            isError =
                promptError != null,

            supportingText =
                promptError?.let {

                    {
                        Text(
                            stringResource(
                                R.string.field_cannot_be_empty
                            )
                        )
                    }
                },
        )

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            FilledIconButton(
                onClick = onVoiceInput,
                enabled = enabled,
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Mic,

                    contentDescription =
                        "Voice input",
                )
            }

            FilledIconButton(
                onClick = onSend,
                enabled = enabled,
            ) {

                Icon(
                    imageVector =
                        Icons.AutoMirrored.Filled.Send,

                    contentDescription =
                        stringResource(
                            R.string.send
                        ),
                )
            }
        }
    }
}

/**
 * Creates the speech recognition Intent.
 */
private fun createSpeechIntent(): Intent {

    return Intent(
        RecognizerIntent.ACTION_RECOGNIZE_SPEECH
    ).apply {

        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Speak your prompt"
        )
    }
}

/**
 * Checks whether speech recognition
 * is available on the device.
 */
private fun isSpeechRecognitionAvailable(
    context: Context
): Boolean {

    return context.packageManager
        .queryIntentActivities(
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        .isNotEmpty()
}

@Preview(
    showBackground = true
)
@Composable
private fun ChatScreenPreview() {

    GeminiApiComposeStarterTheme {

        ChatScreen(

            state = ChatUiState(

                messages = listOf(

                    ChatMessage(
                        id = "1",
                        text = "Hello Gemini!",
                        isUser = true,
                    ),

                    ChatMessage(
                        id = "2",
                        text = "Hello! How can I help you?",
                        isUser = false,
                    ),
                )
            ),

            onPromptChange = {},

            onSend = {},
        )
    }
}