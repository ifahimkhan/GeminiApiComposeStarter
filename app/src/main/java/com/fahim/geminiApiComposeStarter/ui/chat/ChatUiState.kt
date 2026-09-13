package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
)

enum class PromptError {
    EMPTY
}