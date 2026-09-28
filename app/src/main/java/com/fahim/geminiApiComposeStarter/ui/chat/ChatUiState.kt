package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity

/** Immutable UI state for the conversation flow. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null,
    val hasApiKey: Boolean = true,
)

enum class PromptError {
    EMPTY
}
