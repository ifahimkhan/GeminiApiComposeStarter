package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.chat.components.ChatDrawer
import com.fahim.geminiApiComposeStarter.ui.chat.components.ChatInputBar
import com.fahim.geminiApiComposeStarter.ui.chat.components.ChatMessageItem
import com.fahim.geminiApiComposeStarter.ui.chat.components.ChatTopBar
import com.fahim.geminiApiComposeStarter.ui.chat.components.EmptyChatView
import com.fahim.geminiApiComposeStarter.ui.chat.components.ErrorCard
import com.fahim.geminiApiComposeStarter.ui.chat.components.ThinkingMessageItem
import com.fahim.geminiApiComposeStarter.ui.settings.SettingsScreen
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import kotlinx.coroutines.launch

@Composable
fun ChatRoute(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (state.currentScreen) {
        AppScreen.CHAT -> {
            ChatScreen(
                state = state,
                onPromptChange = viewModel::onPromptChange,
                onSend = { viewModel.onSend() },
                onSuggestionClick = viewModel::onSuggestionClick,
                onRetry = viewModel::onRetry,
                onDismissError = viewModel::onDismissError,
                onNewChat = viewModel::onNewChat,
                onSelectConversation = viewModel::onSelectConversation,
                onRegenerate = viewModel::onRegenerate,
                onNavigateToSettings = viewModel::onNavigateToSettings,
            )
        }
        AppScreen.SETTINGS -> {
            SettingsScreen(
                settings = state.settings,
                onBack = viewModel::onNavigateToChat,
                onThemeToggle = viewModel::onThemeToggle,
                onSaveHistoryToggle = viewModel::onSaveHistoryToggle,
                onClearHistory = viewModel::onClearHistory,
            )
        }
    }
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestionClick: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onNewChat: () -> Unit = {},
    onSelectConversation: (String) -> Unit = {},
    onRegenerate: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Handle back button when drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    // Auto-scroll to latest message or thinking indicator
    LaunchedEffect(state.messages.size, state.isLoading) {
        val totalCount = state.messages.size + if (state.isLoading) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ChatDrawer(
                recentConversations = state.recentConversations,
                currentConversationId = state.conversationId,
                onNewChat = {
                    scope.launch { drawerState.close() }
                    onNewChat()
                },
                onRecentChatClick = { conversationId ->
                    scope.launch { drawerState.close() }
                    onSelectConversation(conversationId)
                },
                onNavigateToSettings = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                },
            )
        },
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            topBar = {
                ChatTopBar(
                    onMenuClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onSettingsClick = onNavigateToSettings,
                )
            },
            bottomBar = {
                ChatInputBar(
                    prompt = state.prompt,
                    isLoading = state.isLoading,
                    onPromptChange = onPromptChange,
                    onSend = onSend,
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                ) {
                    // Chat conversation area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        if (state.messages.isEmpty() && !state.isLoading) {
                            EmptyChatView(
                                onSuggestionClick = onSuggestionClick,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                items(
                                    items = state.messages,
                                    key = { it.id },
                                ) { message ->
                                    ChatMessageItem(
                                        message = message,
                                        onRegenerate = onRegenerate,
                                    )
                                }

                                if (state.isLoading) {
                                    item(key = "loading_thinking_indicator") {
                                        ThinkingMessageItem()
                                    }
                                }
                            }
                        }
                    }

                    // Polished Error Card at the bottom of the conversation area
                    AnimatedVisibility(
                        visible = state.errorMessage != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        ErrorCard(
                            errorMessage = state.errorMessage,
                            onDismiss = onDismissError,
                            onRetry = onRetry,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(),
            onPromptChange = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenActivePreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        text = "Explain database normalization",
                        participant = Participant.USER,
                        timestamp = "10:30 AM",
                    ),
                    ChatMessage(
                        text = "Database normalization is the process of organizing data in a database to reduce redundancy and improve data integrity. It usually involves dividing large tables into smaller tables and defining relationships between them using **Normal Forms** (1NF, 2NF, 3NF).",
                        participant = Participant.GEMINI,
                        timestamp = "10:31 AM",
                    ),
                ),
            ),
            onPromptChange = {},
            onSend = {},
        )
    }
}
