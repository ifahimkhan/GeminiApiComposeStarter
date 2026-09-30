package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val id: Long,
    val text: String,
    val sender: MessageSender
)

enum class MessageSender {
    USER,
    GEMINI
}

@Immutable
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError {
    EMPTY
}