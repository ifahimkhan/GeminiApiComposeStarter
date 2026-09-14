package com.example.myapplication.ui.chat

import com.example.myapplication.data.model.ChatMessage

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val autoScrollEnabled: Boolean = true,
    val darkModeOverride: Boolean? = null
)
