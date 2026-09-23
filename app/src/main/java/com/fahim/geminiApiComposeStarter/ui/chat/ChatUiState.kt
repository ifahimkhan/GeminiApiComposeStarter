package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.MessageRole

/**
 * UI representation of a chat message in the conversation.
 */
data class ChatMessage(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

/**
 * Immutable UI state for the conversational chat screen.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedModel: String = "gemini-3.6-flash",
    val availableModels: List<String> = listOf(
        "gemini-3.6-flash",
        "gemini-3.6-pro",
        "gemini-1.5-flash",
        "gemini-1.5-pro",
    ),
    val themeMode: String = "SYSTEM",
) {
    /** Convenience accessor for the most recent model response. */
    val response: String
        get() = messages.lastOrNull { it.role == MessageRole.MODEL }?.content.orEmpty()
}

enum class PromptError {
    EMPTY,
}
