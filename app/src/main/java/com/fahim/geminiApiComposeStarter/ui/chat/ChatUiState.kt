package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

/**
 * Immutable UI state for the conversational chat interface.
 */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "User",
    val systemPrompt: String = "You are a helpful, witty, and concise AI assistant powered by Gemini.",
    val isSettingsOpen: Boolean = false,
    val hasApiKey: Boolean = true,
)

enum class PromptError {
    EMPTY
}
