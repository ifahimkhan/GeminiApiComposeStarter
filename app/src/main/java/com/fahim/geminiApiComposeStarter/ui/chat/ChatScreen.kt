package com.fahim.geminiApiComposeStarter.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.fahim.geminiApiComposeStarter.data.prefs.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ChatRoute(viewModel: ChatViewModel, windowSizeClass: WindowSizeClass) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (!state.isReady) return

    if (state.username == null) {
        WelcomeScreen(onContinue = viewModel::setUsername)
        return
    }

    ChatScreen(
        state = state,
        windowSizeClass = windowSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onVoiceResult = viewModel::onVoiceResult,
        onNewConversation = viewModel::onNewConversation,
        onSelectConversation = viewModel::onSelectConversation,
        onDeleteConversation = viewModel::onDeleteConversation,
        onOpenSettings = viewModel::openSettings,
        onCloseSettings = viewModel::closeSettings,
        onSaveKey = viewModel::saveApiKey,
        onResetKey = viewModel::resetApiKey,
        onThemeChange = viewModel::setThemeMode,
        onDynamicColorChange = viewModel::setDynamicColorEnabled,
        onErrorShown = viewModel::onErrorMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    windowSizeClass: WindowSizeClass,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceResult: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onSaveKey: (String) -> Unit,
    onResetKey: () -> Unit,
    onThemeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onErrorShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            onErrorShown()
        }
    }

    if (state.showSettingsDialog) {
        SettingsDialog(
            maskedApiKey = state.maskedApiKey,
            isUsingCustomKey = state.isUsingCustomKey,
            themeMode = state.themeMode,
            dynamicColorEnabled = state.dynamicColorEnabled,
            onSaveKey = onSaveKey,
            onResetKey = onResetKey,
            onThemeChange = onThemeChange,
            onDynamicColorChange = onDynamicColorChange,
            onDismiss = onCloseSettings,
        )
    }

    val drawerContent: @Composable () -> Unit = {
        ConversationSidebar(
            conversations = state.conversations,
            currentConversationId = state.currentConversationId,
            onNewConversation = {
                onNewConversation()
                scope.launch { drawerState.close() }
            },
            onSelectConversation = {
                onSelectConversation(it)
                scope.launch { drawerState.close() }
            },
            onDeleteConversation = onDeleteConversation,
            onOpenSettings = {
                onOpenSettings()
                scope.launch { drawerState.close() }
            },
            asPermanentPanel = isWideScreen,
        )
    }

    val content: @Composable () -> Unit = {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text(state.username ?: "GeminiChat") },
                    navigationIcon = {
                        if (!isWideScreen) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open conversation history")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
            ) {
                MessageList(
                    messages = state.messages,
                    isLoading = state.isLoading,
                    onSuggestionClick = onPromptChange,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onVoiceResult = onVoiceResult,
                )
            }
        }
    }

    if (isWideScreen) {
        PermanentNavigationDrawer(drawerContent = drawerContent, content = content)
    } else {
        ModalNavigationDrawer(drawerState = drawerState, drawerContent = drawerContent, content = content)
    }
}

@Composable
private fun ConversationSidebar(
    conversations: List<ConversationSummary>,
    currentConversationId: Long?,
    onNewConversation: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    asPermanentPanel: Boolean,
) {
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = "Conversations",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            )
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                label = { Text("New chat") },
                selected = currentConversationId == null,
                onClick = onNewConversation,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(conversations, key = { it.id }) { conversation ->
                    NavigationDrawerItem(
                        label = { Text(conversation.title, maxLines = 1) },
                        selected = conversation.id == currentConversationId,
                        onClick = { onSelectConversation(conversation.id) },
                        badge = {
                            IconButton(onClick = { onDeleteConversation(conversation.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete conversation")
                            }
                        },
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                label = { Text("Settings") },
                selected = false,
                onClick = onOpenSettings,
            )
        }
    }

    if (asPermanentPanel) {
        PermanentDrawerSheet(modifier = Modifier.width(300.dp)) { body() }
    } else {
        ModalDrawerSheet { body() }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty() && !isLoading) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "What can I help you with?",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Choose a suggestion to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            )

            SuggestionButton(
                icon = "💡",
                title = "Learn something",
                prompt = "Explain quantum computing in simple terms",
                onClick = onSuggestionClick,
            )

            SuggestionButton(
                icon = "📝",
                title = "Write something",
                prompt = "Help me write a professional email",
                onClick = onSuggestionClick,
            )

            SuggestionButton(
                icon = "💻",
                title = "Help with coding",
                prompt = "Help me debug my code",
                onClick = onSuggestionClick,
            )

            SuggestionButton(
                icon = "✈️",
                title = "Plan a trip",
                prompt = "Help me plan a weekend trip",
                onClick = onSuggestionClick,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                messages,
                key = { it.id }
            ) { message ->
                ChatBubble(message)
            }

            if (isLoading) {
                item(key = "loading") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .height(20.dp)
                                    .width(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionButton(
    icon: String,
    title: String,
    prompt: String,
    onClick: (String) -> Unit,
) {
    androidx.compose.material3.OutlinedButton(
        onClick = {
            onClick(prompt)
        },
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                modifier = Modifier.padding(end = 12.dp),
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )

                Text(
                    text = prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isFromUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = message.content.toBoldAnnotatedString(),
                color = textColor,
                modifier = Modifier.padding(12.dp),
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
    onVoiceResult: (String) -> Unit,
) {
    val context = LocalContext.current
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            text?.let(onVoiceResult)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                label = { Text("Message Gemini") },
                minLines = 1,
                maxLines = 5,
                enabled = enabled,
                isError = promptError != null,
                supportingText = promptError?.let { { Text("Field cannot be empty") } },
            )
            OutlinedIconButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your prompt")
                    }
                    runCatching { voiceLauncher.launch(intent) }
                },
                enabled = enabled,
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Voice input")
            }
            FilledIconButton(onClick = onSend, enabled = enabled) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
