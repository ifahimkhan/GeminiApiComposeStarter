package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isInitializing: Boolean = true,
    val isClearing: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val showClearConfirmation: Boolean = false,
)

enum class PromptError { EMPTY }
