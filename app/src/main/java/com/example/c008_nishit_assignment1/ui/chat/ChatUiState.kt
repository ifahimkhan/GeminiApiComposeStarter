package com.example.c008_nishit_assignment1.ui.chat

/**
 * UI state holding all state variables for the Chat Screen.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPrompt: String = "",
    val isDarkMode: Boolean? = null
)
