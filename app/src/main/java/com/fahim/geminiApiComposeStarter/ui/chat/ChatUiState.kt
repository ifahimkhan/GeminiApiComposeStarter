package com.fahim.geminiApiComposeStarter.ui.chat

data class ChatSessionState(
    val id: String,
    val title: String
)

/** Immutable UI state for the chat interface. */
data class ChatUiState(
    val sessions: List<ChatSessionState> = emptyList(),
    val currentSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isVoiceAvailable: Boolean = true,
    val autoScrollEnabled: Boolean = true,
    val isDarkMode: Boolean? = null,
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }
