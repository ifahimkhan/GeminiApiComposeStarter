package com.devraj.geminiassistant.ui.chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devraj.geminiassistant.data.local.entity.ChatMessageEntity
import com.devraj.geminiassistant.ui.chat.components.ChatBubble
import com.devraj.geminiassistant.ui.chat.components.ChatInputBar
import com.devraj.geminiassistant.ui.chat.components.SettingsDialog
import com.devraj.geminiassistant.util.TtsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val QUICK_PROMPTS = listOf(
    "Explain Kotlin Coroutines in simple terms",
    "How does Jetpack Compose state work?",
    "Write a sample Room DAO implementation",
    "Best practices for Android Keystore encryption",
    "Give me an interesting Android development fact"
)

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val ttsManager = remember { TtsManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenMatches?.firstOrNull() ?: ""
            if (recognizedText.isNotEmpty()) {
                viewModel.onVoiceResult(recognizedText)
            }
        }
    }

    val onLaunchVoiceInput: () -> Unit = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech Recognition not available", Toast.LENGTH_SHORT).show()
        }
    }

    val onExportChat: () -> Unit = {
        if (state.messages.isEmpty()) {
            Toast.makeText(context, "No messages to export", Toast.LENGTH_SHORT).show()
        } else {
            val exportBuilder = StringBuilder()
            exportBuilder.append("# Gemini AI Assistant Conversation Transcript\n")
            exportBuilder.append("Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
            exportBuilder.append("Model: ${state.selectedModel}\n\n---\n\n")

            state.messages.forEach { msg ->
                val sender = if (msg.isUser) "**User (Devraj N066)**" else "**Gemini AI**"
                val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp))
                exportBuilder.append("$sender _($time)_:\n${msg.text}\n\n")
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, exportBuilder.toString())
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Export Conversation"))
        }
    }

    val onSpeakMessage: (ChatMessageEntity) -> Unit = { msg ->
        viewModel.setSpeakingMessageId(msg.id)
        ttsManager.speak(msg.text) {
            viewModel.setSpeakingMessageId(null)
        }
    }

    val onStopSpeaking: () -> Unit = {
        ttsManager.stop()
        viewModel.setSpeakingMessageId(null)
    }

    ChatScreen(
        state = state,
        windowWidthSizeClass = windowWidthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onStopGeneration = viewModel::stopGeneration,
        onVoiceInputClick = onLaunchVoiceInput,
        onClearChat = viewModel::clearChat,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onSaveSettings = viewModel::saveSettings,
        onDismissError = viewModel::dismissError,
        onExportChat = onExportChat,
        onToggleSearch = viewModel::toggleSearch,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onSpeakMessage = onSpeakMessage,
        onStopSpeaking = onStopSpeaking
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopGeneration: () -> Unit = {},
    onVoiceInputClick: () -> Unit = {},
    onClearChat: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onCloseSettings: () -> Unit = {},
    onSaveSettings: (String, Float, String) -> Unit = { _, _, _ -> },
    onDismissError: () -> Unit = {},
    onExportChat: () -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSpeakMessage: (ChatMessageEntity) -> Unit = {},
    onStopSpeaking: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val filteredMessages = remember(state.messages, state.searchQuery) {
        if (state.searchQuery.isBlank()) {
            state.messages
        } else {
            state.messages.filter { it.text.contains(state.searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(filteredMessages.size, state.streamingResponse) {
        if (filteredMessages.isNotEmpty() || state.streamingResponse.isNotEmpty()) {
            val target = (filteredMessages.size + if (state.streamingResponse.isNotEmpty()) 1 else 0) - 1
            if (target >= 0) {
                listState.animateScrollToItem(target)
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    if (state.isSettingsOpen) {
        SettingsDialog(
            currentInstruction = state.systemInstruction,
            currentTemperature = state.temperature,
            currentModel = state.selectedModel,
            onDismiss = onCloseSettings,
            onSave = onSaveSettings
        )
    }

    val isExpandedOrMedium = windowWidthSizeClass == WindowWidthSizeClass.Expanded ||
            windowWidthSizeClass == WindowWidthSizeClass.Medium

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            if (state.isSearchOpen) {
                CenterAlignedTopAppBar(
                    title = {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search messages...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )
                    },
                    actions = {
                        IconButton(onClick = { onToggleSearch(false) }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Close Search"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = state.selectedModel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onToggleSearch(true) }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Messages"
                            )
                        }
                        IconButton(onClick = onExportChat) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Chat"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                        IconButton(onClick = onClearChat) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (isExpandedOrMedium) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Card(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Assistant Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Active Model:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = state.selectedModel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "System Prompt:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = state.systemInstruction,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Temperature: ${state.temperature}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (state.lastLatencyMs != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Last Latency: ${state.lastLatencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                ) {
                    ChatContent(
                        state = state,
                        messages = filteredMessages,
                        listState = listState,
                        onPromptSelected = onPromptChange,
                        onSpeakMessage = onSpeakMessage,
                        onStopSpeaking = onStopSpeaking,
                        modifier = Modifier.weight(1f)
                    )
                    ChatInputBar(
                        prompt = state.prompt,
                        onPromptChange = onPromptChange,
                        onSend = onSend,
                        onVoiceInputClick = onVoiceInputClick,
                        isLoading = state.isLoading,
                        onStopGeneration = onStopGeneration
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                ChatContent(
                    state = state,
                    messages = filteredMessages,
                    listState = listState,
                    onPromptSelected = onPromptChange,
                    onSpeakMessage = onSpeakMessage,
                    onStopSpeaking = onStopSpeaking,
                    modifier = Modifier.weight(1f)
                )

                AnimatedVisibility(visible = state.messages.isEmpty() && !state.isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QUICK_PROMPTS.forEach { promptSuggestion ->
                            AssistChip(
                                onClick = { onPromptChange(promptSuggestion) },
                                label = { Text(promptSuggestion, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                ChatInputBar(
                    prompt = state.prompt,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceInputClick = onVoiceInputClick,
                    isLoading = state.isLoading,
                    onStopGeneration = onStopGeneration
                )
            }
        }
    }
}

@Composable
private fun ChatContent(
    state: ChatUiState,
    messages: List<ChatMessageEntity>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onPromptSelected: (String) -> Unit = {},
    onSpeakMessage: (ChatMessageEntity) -> Unit = {},
    onStopSpeaking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty() && state.streamingResponse.isEmpty() && !state.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Hello Devraj (N066)! How can I help you today?",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ask any question, try code generation, or speak using voice input.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
        ) {
            items(
                items = messages,
                key = { message -> message.id }
            ) { message ->
                ChatBubble(
                    message = message,
                    isSpeaking = (state.speakingMessageId == message.id),
                    onSpeakClick = onSpeakMessage,
                    onStopSpeakClick = onStopSpeaking
                )
            }

            // Real-time streaming response bubble
            if (state.streamingResponse.isNotEmpty()) {
                item(key = "streaming_live_bubble") {
                    ChatBubble(
                        message = ChatMessageEntity(
                            id = -1L,
                            text = state.streamingResponse,
                            isUser = false,
                            timestamp = System.currentTimeMillis()
                        ),
                        isSpeaking = false
                    )
                }
            } else if (state.isLoading) {
                item(key = "loading_indicator") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini is generating streaming response...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
