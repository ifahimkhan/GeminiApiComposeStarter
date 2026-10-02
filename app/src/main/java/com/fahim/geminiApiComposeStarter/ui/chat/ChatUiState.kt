package com.fahim.geminiApiComposeStarter.ui.chat

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val promptError: PromptError? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val messages: List<ChatMessage> = emptyList()
)

enum class PromptError {
    EMPTY
}