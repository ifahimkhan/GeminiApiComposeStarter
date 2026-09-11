package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.R
import com.fahim.geminiApiComposeStarter.data.local.Message
import com.fahim.geminiApiComposeStarter.ui.text.toMarkdownAnnotatedString
import com.fahim.geminiApiComposeStarter.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    onVoiceInput: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatScreen(
        uiState = uiState,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onNewChat = viewModel::onNewChat,
        onSelectConversation = viewModel::onSelectConversation,
        onDeleteConversation = viewModel::deleteConversation,
        onFork = viewModel::onFork,
        onSelectBranch = viewModel::onSelectBranch,
        onStopStreaming = viewModel::stopStreaming,
        onVoiceInput = onVoiceInput
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatScreen(
    uiState: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onNewChat: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onFork: (Long) -> Unit,
    onSelectBranch: (String) -> Unit,
    onStopStreaming: () -> Unit,
    onVoiceInput: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerState = drawerState,
                drawerContainerColor = DarkSurface,
                drawerContentColor = White
            ) {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("New Chat", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        onNewChat()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = GeminiBubbleBorderColor)
                LazyColumn {
                    items(uiState.conversations) { conv ->
                        NavigationDrawerItem(
                            label = { Text(conv.title, maxLines = 1) },
                            selected = uiState.currentConversationId == conv.id,
                            onClick = {
                                onSelectConversation(conv.id)
                                scope.launch { drawerState.close() }
                            },
                            badge = {
                                IconButton(onClick = { onDeleteConversation(conv.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gemini AI", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            StatusDot(isLoading = uiState.isLoading || uiState.isStreaming)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNewChat) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = White,
                        navigationIconContentColor = White,
                        actionIconContentColor = White
                    )
                )
            },
            containerColor = DarkBackground
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.branches.size > 1) {
                    BranchSelector(
                        branches = uiState.branches,
                        currentBranch = uiState.currentBranchId,
                        onSelectBranch = onSelectBranch
                    )
                }

                val listState = rememberLazyListState()
                LaunchedEffect(uiState.messages.size, uiState.streamingText) {
                    if (uiState.messages.isNotEmpty()) {
                        listState.animateScrollToItem(uiState.messages.size)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.messages.isEmpty() && !uiState.isStreaming) {
                        item { EmptyState(onSuggestionClick = { onPromptChange(it); onSend() }) }
                    }

                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onFork = { onFork(message.id) }
                        )
                    }

                    if (uiState.errorMessage != null) {
                        item {
                            ErrorBubble(
                                message = uiState.errorMessage ?: "Unknown error",
                                onRetry = onSend
                            )
                        }
                    }

                    if (uiState.isStreaming) {
                        item {
                            MessageBubble(
                                message = Message(
                                    role = "model",
                                    content = uiState.streamingText,
                                    conversationId = 0
                                ),
                                isStreaming = true
                            )
                        }
                    }
                }

                ChatInputBar(
                    prompt = uiState.prompt,
                    isStreaming = uiState.isStreaming || uiState.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                    onStop = onStopStreaming,
                    onVoiceInput = onVoiceInput
                )
            }
        }
    }
}

@Composable
private fun StatusDot(isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(if (isLoading) PrimaryPurple.copy(alpha = alpha) else Color.Green)
    )
}

@Composable
private fun BranchSelector(
    branches: List<String>,
    currentBranch: String,
    onSelectBranch: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        branches.forEach { branch ->
            val selected = branch == currentBranch
            Surface(
                onClick = { onSelectBranch(branch) },
                shape = RoundedCornerShape(16.dp),
                color = if (selected) PrimaryPurple else DarkSurface,
                border = if (selected) null else BorderStroke(1.dp, GeminiBubbleBorderColor),
                modifier = Modifier.height(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(branch, fontSize = 12.sp, color = White)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isStreaming: Boolean = false,
    onFork: (() -> Unit)? = null
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    var showMenu by remember { mutableStateOf(false) }

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) UserBubbleColor else GeminiBubbleColor
    val shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 2.dp,
        bottomEnd = if (isUser) 2.dp else 18.dp
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(shape)
                    .border(
                        width = if (isUser) 0.dp else 1.dp,
                        color = if (isUser) Color.Transparent else GeminiBubbleBorderColor,
                        shape = shape
                    )
                    .clickable { showMenu = true },
                color = bubbleColor,
                shape = shape
            ) {
                Text(
                    text = message.content.toMarkdownAnnotatedString(),
                    modifier = Modifier.padding(12.dp),
                    color = White,
                    fontSize = 15.sp
                )
            }
            
            if (!isUser && !isStreaming) {
                IconButton(onClick = { onFork?.invoke() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ForkRight, contentDescription = "Fork", tint = TextGray, modifier = Modifier.size(16.dp))
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    clipboardManager.setText(AnnotatedString(message.content))
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun ErrorBubble(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = ErrorRed.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, color = White, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onRetry,
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryPurple)
            ) {
                Text("Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    prompt: String,
    isStreaming: Boolean,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onVoiceInput: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        color = DarkBackground,
        tonalElevation = 8.dp
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
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, PrimaryPurple.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                placeholder = { Text("Type something...", color = TextGray) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PrimaryPurple,
                    focusedTextColor = White,
                    unfocusedTextColor = White
                ),
                maxLines = 4,
                trailingIcon = {
                    IconButton(onClick = onVoiceInput) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice", tint = TextGray)
                    }
                }
            )

            Spacer(Modifier.width(8.dp))

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.8f else 1f)

            FloatingActionButton(
                onClick = { if (isStreaming) onStop() else onSend() },
                containerColor = PrimaryPurple,
                contentColor = White,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale),
                interactionSource = interactionSource
            ) {
                Icon(
                    imageVector = if (isStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = PrimaryPurple,
            modifier = Modifier
                .size(64.dp)
                .border(2.dp, PrimaryPurple.copy(alpha = 0.2f), RoundedCornerShape(50))
                .padding(16.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Ask Gemini anything", color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(32.dp))
        
        val suggestions = listOf("Explain quantum computing", "Write a short poem", "How to cook pasta?")
        suggestions.forEach { suggestion ->
            Surface(
                onClick = { onSuggestionClick(suggestion) },
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth(0.8f),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, GeminiBubbleBorderColor)
            ) {
                Text(suggestion, modifier = Modifier.padding(16.dp), color = TextGray, fontSize = 14.sp)
            }
        }
    }
}
