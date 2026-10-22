package com.example.geminiapicomposestarter.ui.chat

import com.example.geminiapicomposestarter.data.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isDarkMode: Boolean = false,
    val currentPrompt: String = ""
)
