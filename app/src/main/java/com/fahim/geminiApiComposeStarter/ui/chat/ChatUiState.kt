package com.fahim.geminiApiComposeStarter.ui.chat

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }