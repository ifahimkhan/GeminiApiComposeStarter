package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity

/**
 * Immutable UI state model for the Chat screen.
 * Collected by Jetpack Compose via StateFlow with lifecycle-awareness.
 */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
    val lastFailedPrompt: String? = null
)

enum class PromptError {
    EMPTY
}
