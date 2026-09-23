package com.example.n083harshitraiassignment1.model

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val userName: String = "User",
    val tone: String = "Balanced"
)