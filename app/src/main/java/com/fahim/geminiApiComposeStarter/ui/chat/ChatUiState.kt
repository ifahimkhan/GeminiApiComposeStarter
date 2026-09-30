package com.fahim.geminiApiComposeStarter.ui.chat

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val darkMode: Boolean? = null,
)

enum class PromptError {
    EMPTY
}