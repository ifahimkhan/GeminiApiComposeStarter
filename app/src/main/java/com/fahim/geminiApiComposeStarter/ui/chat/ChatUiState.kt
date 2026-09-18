package com.fahim.geminiApiComposeStarter.ui.chat

/**
 * Immutable UI state for the Gemini chat experience.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedModel: String = "gemini-3.6-flash",
    val isDarkTheme: Boolean? = null, // null = system default, true = dark, false = light
)

data class ChatMessage(
    val id: Long = 0,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

enum class MessageSender {
    USER,
    GEMINI,
}

enum class PromptError {
    EMPTY,
}
