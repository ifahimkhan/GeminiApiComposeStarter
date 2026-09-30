package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "User"
)

enum class PromptError { EMPTY }