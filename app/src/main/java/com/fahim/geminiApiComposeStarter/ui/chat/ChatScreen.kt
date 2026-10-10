package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fahim.geminiApiComposeStarter.ui.components.ChatBubble
import com.fahim.geminiApiComposeStarter.ui.components.ChatInputBar
import com.fahim.geminiApiComposeStarter.ui.components.EmptyState
import com.fahim.geminiApiComposeStarter.ui.components.F1Backdrop
import com.fahim.geminiApiComposeStarter.ui.components.PitwallTopBar
import com.fahim.geminiApiComposeStarter.ui.theme.F1Red
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import com.fahim.geminiApiComposeStarter.ui.theme.HairlineOutline

@Composable
fun ChatRoute(
    viewModel: ChatViewModel,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        windowWidthSizeClass = windowWidthSizeClass,
        onPromptChange = viewModel::onPromptChange,
        onSend = viewModel::onSend,
        onClearChat = viewModel::onClearChat,
        onErrorDismissed = viewModel::onErrorDismissed,
        onChipSelected = viewModel::onChipSelected,
    )
}

@Composable
fun ChatScreen(
    state: ChatUiState,
    windowWidthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onErrorDismissed: () -> Unit,
    onChipSelected: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // Show error snackbar with retry
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "RETRY"
            )
            if (result == SnackbarResult.ActionPerformed) {
                onErrorDismissed()
            } else {
                onErrorDismissed()
            }
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(
                index = state.messages.size - 1,
                scrollOffset = 0
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            PitwallTopBar(
                isLoading = state.isLoading,
                isError = state.errorMessage != null,
                onClearChat = onClearChat
            )
        },
        bottomBar = {
            ChatInputBar(
                prompt = state.prompt,
                isLoading = state.isLoading,
                onPromptChange = onPromptChange,
                onSend = onSend
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.border(width = 2.dp, color = F1Red),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = F1Red
                )
            }
        }
    ) { innerPadding ->

        val isExpanded = windowWidthSizeClass != WindowWidthSizeClass.Compact

        F1Backdrop {
            // Responsive layout: medium/expanded constrains to max 720dp centered
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpanded) {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Box(
                    modifier = if (isExpanded) {
                        Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    if (state.messages.isEmpty() && !state.isLoading) {
                        EmptyState(
                            onChipClick = onChipSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                        ) {
                            items(
                                items = state.messages,
                                key = { it.id }
                            ) { message ->
                                ChatBubble(
                                    isUser = message.sender == "DRIVER",
                                    text = message.text,
                                    timestamp = message.timestamp,
                                    isLoading = message.isLoading
                                )
                            }
                        }
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
private fun ChatScreenEmptyPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(),
            onPromptChange = {},
            onSend = {},
            onClearChat = {},
            onErrorDismissed = {},
            onChipSelected = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0D)
@Composable
private fun ChatScreenConversationPreview() {
    GeminiApiComposeStarterTheme {
        ChatScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = 1L,
                        sender = "DRIVER",
                        text = "Explain DRS",
                        timestamp = "14:31"
                    ),
                    ChatMessage(
                        id = 2L,
                        sender = "PITWALL",
                        text = "**DRS** (Drag Reduction System) is a movable rear wing element. Activated when a driver is within **1 second** of the car ahead at designated detection points. Reduces aerodynamic drag for a speed boost on straights.",
                        timestamp = "14:32"
                    )
                ),
                isLoading = false
            ),
            onPromptChange = {},
            onSend = {},
            onClearChat = {},
            onErrorDismissed = {},
            onChipSelected = {}
        )
    }
}
