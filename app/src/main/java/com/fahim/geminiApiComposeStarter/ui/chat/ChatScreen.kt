package com.fahim.geminiApiComposeStarter.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.ui.icons.MicIcon
import com.fahim.geminiApiComposeStarter.ui.text.toBoldAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = state.conversations,
                currentId = state.currentConversationId,
                onSelect = { id ->
                    viewModel.onSelectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.onNewChat()
                    scope.launch { drawerState.close() }
                },
                onDelete = { id -> viewModel.onDeleteConversation(id) },
            )
        },
    ) {
        ChatScreen(
            state = state,
            onPromptChange = viewModel::onPromptChange,
            onSend = viewModel::onSend,
            onErrorShown = viewModel::onErrorShown,
            onToggleCompactBubbles = viewModel::onToggleCompactBubbles,
            onSetThemeMode = viewModel::onSetThemeMode,
            onOpenDrawer = { scope.launch { drawerState.open() } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onErrorShown: () -> Unit,
    onToggleCompactBubbles: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onShowMessage: (String) -> Unit = remember(scope, snackbarHostState) {
        { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onErrorShown()
    }

    var settingsExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactHeight = maxHeight < 480.dp
        val isWide = maxWidth >= 600.dp

        val horizontalPadding = if (isWide) 24.dp else 12.dp
        val promptMaxLines = if (isCompactHeight) 3 else 5

        val widthFraction = if (state.compactBubbles) 0.70f else 0.85f
        val maxBubbleWidth: Dp = minOf(maxWidth * widthFraction, 640.dp)

        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Open chats",
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        val initial = stringResource(R.string.user_name)
                                            .trim()
                                            .firstOrNull()
                                            ?.uppercase()
                                            ?: "?"
                                        Text(
                                            text = initial,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.user_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { settingsExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = stringResource(R.string.settings),
                                )
                            }
                            SettingsMenu(
                                expanded = settingsExpanded,
                                onDismiss = { settingsExpanded = false },
                                themeMode = state.themeMode,
                                onSetThemeMode = onSetThemeMode,
                                compactBubbles = state.compactBubbles,
                                onToggleCompactBubbles = onToggleCompactBubbles,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (state.messages.isEmpty()) {
                        EmptyState(
                            onPickSuggestion = onPromptChange,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        MessageList(
                            messages = state.messages,
                            maxBubbleWidth = maxBubbleWidth,
                            compact = state.compactBubbles,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (state.isLoading) {
                        LoadingOverlay(modifier = Modifier.align(Alignment.Center))
                    }
                }

                PromptBar(
                    prompt = state.prompt,
                    promptError = state.promptError,
                    enabled = !state.isLoading,
                    minLines = 1,
                    maxLines = promptMaxLines,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onShowMessage = onShowMessage,
                )
            }
        }
    }
}

@Composable
private fun SettingsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    onSetThemeMode: (ThemeMode) -> Unit,
    compactBubbles: Boolean,
    onToggleCompactBubbles: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = {
                Text(
                    text = stringResource(R.string.theme),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onClick = {},
            enabled = false,
        )
        ThemeOptionRow(
            label = stringResource(R.string.theme_system),
            selected = themeMode == ThemeMode.SYSTEM,
            onClick = { onSetThemeMode(ThemeMode.SYSTEM); onDismiss() },
        )
        ThemeOptionRow(
            label = stringResource(R.string.theme_light),
            selected = themeMode == ThemeMode.LIGHT,
            onClick = { onSetThemeMode(ThemeMode.LIGHT); onDismiss() },
        )
        ThemeOptionRow(
            label = stringResource(R.string.theme_dark),
            selected = themeMode == ThemeMode.DARK,
            onClick = { onSetThemeMode(ThemeMode.DARK); onDismiss() },
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.compact_bubbles)) },
            onClick = { onToggleCompactBubbles() },
            trailingIcon = {
                Switch(
                    checked = compactBubbles,
                    onCheckedChange = { onToggleCompactBubbles() },
                )
            },
        )
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface,
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationDrawer(
    conversations: List<Conversation>,
    currentId: Long?,
    onSelect: (Long) -> Unit,
    onNewChat: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Chats",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onNewChat,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New chat")
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    items(items = conversations, key = { it.id }) { conversation ->
                        val selected = conversation.id == currentId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { onSelect(conversation.id) },
                                    onLongClick = { onDelete(conversation.id) },
                                ),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = conversation.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                    color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                                IconButton(onClick = { onDelete(conversation.id) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Delete conversation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    onPickSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_assistant),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_greeting),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        SuggestionChip(stringResource(R.string.suggestion_1), onPickSuggestion)
        Spacer(Modifier.height(8.dp))
        SuggestionChip(stringResource(R.string.suggestion_2), onPickSuggestion)
        Spacer(Modifier.height(8.dp))
        SuggestionChip(stringResource(R.string.suggestion_3), onPickSuggestion)
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: (String) -> Unit) {
    val shape = remember { RoundedCornerShape(24.dp) }
    val handleClick = remember(text, onClick) { { onClick(text) } }
    Surface(
        onClick = handleClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun LoadingOverlay(modifier: Modifier = Modifier) {
    val shape = remember { RoundedCornerShape(28.dp) }
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.generating_response),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    maxBubbleWidth: Dp,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(items = messages, key = { it.id }) { message ->
            MessageBubble(
                message = message,
                maxBubbleWidth = maxBubbleWidth,
                compact = compact,
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    maxBubbleWidth: Dp,
    compact: Boolean,
) {
    val isUser = message.author == ChatAuthor.USER

    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant

    val bubbleShape = remember(isUser) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = if (isUser) 20.dp else 6.dp,
            bottomEnd = if (isUser) 6.dp else 20.dp,
        )
    }

    val horizontalBubblePadding = if (compact) 10.dp else 14.dp
    val verticalBubblePadding = if (compact) 6.dp else 10.dp
    val iconSize = if (compact) 16.dp else 20.dp

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = maxBubbleWidth),
            shape = bubbleShape,
            color = bubbleColor,
            contentColor = contentColor,
            tonalElevation = if (isUser) 0.dp else 1.dp,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = horizontalBubblePadding,
                    vertical = verticalBubblePadding,
                ),
                verticalAlignment = Alignment.Top,
            ) {
                if (!isUser) {
                    Icon(
                        painter = painterResource(R.drawable.ic_assistant),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
                }
                Text(
                    text = message.text.toBoldAnnotatedString(),
                    style = if (compact) MaterialTheme.typography.bodyMedium
                    else MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun PromptBar(
    prompt: String,
    promptError: PromptError?,
    enabled: Boolean,
    minLines: Int,
    maxLines: Int,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val context = LocalContext.current

    val currentPrompt by rememberUpdatedState(prompt)
    val currentOnPromptChange by rememberUpdatedState(onPromptChange)
    val currentOnShowMessage by rememberUpdatedState(onShowMessage)

    val listeningLabel by rememberUpdatedState(stringResource(R.string.listening_active))
    val permissionDeniedMessage by rememberUpdatedState(
        stringResource(R.string.mic_permission_denied)
    )
    val speechUnavailableMessage by rememberUpdatedState(
        stringResource(R.string.speech_recognition_unavailable)
    )
    val noMatchMessage by rememberUpdatedState(stringResource(R.string.speech_no_match))
    val voiceInputDescription = stringResource(R.string.voice_input)

    var isListening by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (recognized.isNotEmpty()) {
                val existing = currentPrompt
                val combined =
                    if (existing.isBlank()) recognized else "$existing $recognized"
                currentOnPromptChange(combined)
            } else {
                currentOnShowMessage(noMatchMessage)
            }
        }
    }

    val startListening: () -> Unit = remember(speechLauncher) {
        {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_PROMPT, listeningLabel)
            }
            try {
                isListening = true
                speechLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                isListening = false
                currentOnShowMessage(speechUnavailableMessage)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startListening()
        else currentOnShowMessage(permissionDeniedMessage)
    }

    val onMicClick: () -> Unit = remember(context, permissionLauncher, startListening) {
        {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startListening()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val micTint = if (isListening) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurfaceVariant

    val inputShape = remember { RoundedCornerShape(24.dp) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.enter_your_prompt_here)) },
            minLines = minLines,
            maxLines = maxLines,
            enabled = enabled,
            isError = promptError != null,
            shape = inputShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            supportingText = promptError?.let {
                { Text(stringResource(R.string.field_cannot_be_empty)) }
            },
        )

        Spacer(Modifier.width(6.dp))

        IconButton(
            onClick = onMicClick,
            enabled = enabled,
        ) {
            Icon(
                imageVector = MicIcon,
                contentDescription = voiceInputDescription,
                tint = micTint,
            )
        }

        Spacer(Modifier.width(4.dp))

        FilledIconButton(
            onClick = onSend,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
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
                    ChatMessage(id = 0, text = "Hi! What is Compose?", author = ChatAuthor.USER),
                    ChatMessage(
                        id = 1,
                        text = "**Compose** is Android's modern declarative UI toolkit.",
                        author = ChatAuthor.GEMINI,
                    ),
                ),
            ),
            onPromptChange = {},
            onSend = {},
            onErrorShown = {},
            onToggleCompactBubbles = {},
            onSetThemeMode = {},
            onOpenDrawer = {},
        )
    }
}