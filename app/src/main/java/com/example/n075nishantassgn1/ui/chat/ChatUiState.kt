package com.example.n075nishantassgn1.ui.chat

data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)