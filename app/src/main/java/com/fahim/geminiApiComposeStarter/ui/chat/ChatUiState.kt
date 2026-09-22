package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.mascot.MascotState
import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode

/**
 * Immutable UI state for the chat screen. Everything the screen needs to draw itself lives here,
 * so every composable below [ChatRoute] is stateless and directly previewable.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val mascotState: MascotState = MascotState.IDLE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
)

enum class PromptError { EMPTY }
