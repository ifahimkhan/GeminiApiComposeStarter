package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity

/**
 * Immutable UI state for the conversational chat flow.
 * State is hoisted from ChatViewModel and exposed as a StateFlow.
 */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "Krris",
)

enum class PromptError {
    EMPTY
}
