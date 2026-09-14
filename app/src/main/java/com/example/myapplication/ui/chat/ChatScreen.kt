package com.example.myapplication.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatRole
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onSpeechRecognized(spokenText)
            }
        }
    }

    ChatScreenContent(
        uiState = uiState,
        onInputChanged = viewModel::onInputTextChanged,
        onSendClicked = viewModel::sendMessage,
        onVoiceClicked = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message")
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "Speech recognition is not available on this device",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onClearHistoryClicked = viewModel::clearHistory,
        onToggleDarkModeClicked = {
            val nextDarkMode = when (uiState.darkModeOverride) {
                true -> false
                false -> null
                null -> true
            }
            viewModel.setDarkMode(nextDarkMode)
        },
        onClearError = viewModel::clearError,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreenContent(
    uiState: ChatUiState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onVoiceClicked: () -> Unit,
    onClearHistoryClicked: () -> Unit,
    onToggleDarkModeClicked: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val error = uiState.errorMessage
        if (error != null) {
            snackbarHostState.showSnackbar(error)
            onClearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Gemini AI Chatbot",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleDarkModeClicked) {
                        Icon(
                            imageVector = if (uiState.darkModeOverride == true) {
                                Icons.Default.LightMode
                            } else {
                                Icons.Default.DarkMode
                            },
                            contentDescription = "Toggle Dark Mode"
                        )
                    }
                    IconButton(onClick = onClearHistoryClicked) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat History"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                ChatMessageList(
                    messages = uiState.messages,
                    isLoading = uiState.isLoading,
                    autoScrollEnabled = uiState.autoScrollEnabled,
                    modifier = Modifier.widthIn(max = 800.dp)
                )
            }

            ChatInputBar(
                inputText = uiState.inputText,
                isLoading = uiState.isLoading,
                onInputChanged = onInputChanged,
                onSendClicked = onSendClicked,
                onVoiceClicked = onVoiceClicked,
                modifier = Modifier.widthIn(max = 800.dp)
            )
        }
    }
}

@Composable
fun ChatMessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    autoScrollEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (autoScrollEnabled && (messages.isNotEmpty() || isLoading)) {
            val totalItems = messages.size + if (isLoading) 1 else 0
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.size(8.dp)) }

        items(
            items = messages,
            key = { message -> message.id }
        ) { message ->
            ChatMessageBubble(message = message)
        }

        if (isLoading) {
            item(key = "loading_indicator") {
                LoadingBubble()
            }
        }

        item { Spacer(modifier = Modifier.size(8.dp)) }
    }
}

@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Gemini",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.padding(12.dp)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun LoadingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Gemini",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("loading_indicator"),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Gemini is typing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onVoiceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                placeholder = { Text("Ask Gemini anything...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 4,
                trailingIcon = {
                    IconButton(
                        onClick = onVoiceClicked,
                        modifier = Modifier.testTag("voice_input_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClicked,
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isLoading) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    )
                    .testTag("send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isLoading) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Light Theme Completed Chat")
@Composable
fun ChatScreenPreview() {
    MyApplicationTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.USER,
                        text = "Hello Gemini! What can you help me with today?"
                    ),
                    ChatMessage(
                        role = ChatRole.MODEL,
                        text = "Hello! I am your AI chatbot powered by Google Gemini. I can help answer questions, write code, brainstorm ideas, and store your chat history locally using Room Database."
                    ),
                    ChatMessage(
                        role = ChatRole.USER,
                        text = "Is my API key stored securely?"
                    ),
                    ChatMessage(
                        role = ChatRole.MODEL,
                        text = "Yes! Your API key is encrypted at rest using AES-256-GCM backed by the Android Keystore, stored securely in DataStore, and decrypted strictly in memory when sending requests."
                    )
                ),
                inputText = "That sounds great!"
            ),
            onInputChanged = {},
            onSendClicked = {},
            onVoiceClicked = {},
            onClearHistoryClicked = {},
            onToggleDarkModeClicked = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Dark Theme Chat", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChatScreenDarkPreview() {
    MyApplicationTheme(darkTheme = true) {
        ChatScreenContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(role = ChatRole.USER, text = "What is the capital of France?"),
                    ChatMessage(role = ChatRole.MODEL, text = "The capital of France is Paris. It is famous for iconic landmarks like the Eiffel Tower, the Louvre Museum, and Notre-Dame Cathedral."),
                    ChatMessage(role = ChatRole.USER, text = "How is dark mode configured in this app?"),
                    ChatMessage(role = ChatRole.MODEL, text = "The app uses isSystemInDarkTheme() combined with Material 3 dynamic color scheme and custom theme overrides.")
                ),
                inputText = "Tell me more!",
                darkModeOverride = true
            ),
            onInputChanged = {},
            onSendClicked = {},
            onVoiceClicked = {},
            onClearHistoryClicked = {},
            onToggleDarkModeClicked = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Loading State")
@Composable
fun ChatScreenLoadingPreview() {
    MyApplicationTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.USER,
                        text = "Explain Kotlin Coroutines and StateFlow in brief."
                    )
                ),
                isLoading = true,
                inputText = ""
            ),
            onInputChanged = {},
            onSendClicked = {},
            onVoiceClicked = {},
            onClearHistoryClicked = {},
            onToggleDarkModeClicked = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "4. Error State with Snackbar")
@Composable
fun ChatScreenErrorPreview() {
    MyApplicationTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(role = ChatRole.USER, text = "Summarize quantum computing.")
                ),
                errorMessage = "Failed to connect to Gemini API. Please check your internet connection and local.properties setup."
            ),
            onInputChanged = {},
            onSendClicked = {},
            onVoiceClicked = {},
            onClearHistoryClicked = {},
            onToggleDarkModeClicked = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "5. Tablet / Wide Screen Layout", widthDp = 1280, heightDp = 800)
@Composable
fun ChatScreenTabletPreview() {
    MyApplicationTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(role = ChatRole.USER, text = "Can you help me design a responsive Jetpack Compose screen?"),
                    ChatMessage(role = ChatRole.MODEL, text = "Absolutely! You can use WindowSizeClass, Modifier.widthIn(max = 800.dp), and LazyColumn to create smooth adaptive layouts for phones, foldables, and tablets.")
                ),
                inputText = "Show me an example code snippet"
            ),
            onInputChanged = {},
            onSendClicked = {},
            onVoiceClicked = {},
            onClearHistoryClicked = {},
            onToggleDarkModeClicked = {},
            onClearError = {}
        )
    }
}

@Preview(showBackground = true, name = "6. NMIMS MPSTME & Akshay Sathaye Q&A Preview")
@Composable
fun ChatScreenCollegeQuestionsPreview() {
    MyApplicationTheme {
        ChatScreenContent(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        role = ChatRole.USER,
                        text = "who is akshay sathaye"
                    ),
                    ChatMessage(
                        role = ChatRole.MODEL,
                        text = "Akshay Sathaye is a Mobile Application Development student at NMIMS MPSTME, working on Android AI projects using Jetpack Compose and Gemini API."
                    ),
                    ChatMessage(
                        role = ChatRole.USER,
                        text = "where is MPSTME college"
                    ),
                    ChatMessage(
                        role = ChatRole.MODEL,
                        text = "Mukesh Patel School of Technology Management & Engineering (MPSTME) is located in Vile Parle West, Mumbai, Maharashtra, India. It is a constituent school of SVKM's NMIMS University."
                    )
                ),
                inputText = "Tell me more about MPSTME courses"
            ),
            onInputChanged = {},
            onSendClicked = {},
            onVoiceClicked = {},
            onClearHistoryClicked = {},
            onToggleDarkModeClicked = {},
            onClearError = {}
        )
    }
}
