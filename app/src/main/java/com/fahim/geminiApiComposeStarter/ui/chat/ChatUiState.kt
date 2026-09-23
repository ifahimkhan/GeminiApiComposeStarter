package com.fahim.geminiApiComposeStarter.ui.chat

data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,

    // DataStore preference
    val responseStyle: String = "Normal"
)

enum class PromptError {
    EMPTY
}

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean
)