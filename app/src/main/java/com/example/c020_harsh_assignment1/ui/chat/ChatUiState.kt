package com.example.c020_harsh_assignment1.ui.chat

import com.example.c020_harsh_assignment1.data.local.ChatMessage

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val promptError: PromptError? = null,
    val responseStyle: String = "Simple",
)

enum class PromptError {
    EMPTY,
}
