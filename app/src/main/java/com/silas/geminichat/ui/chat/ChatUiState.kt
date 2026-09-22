package com.silas.geminichat.ui.chat

import com.silas.geminichat.model.ChatMessage

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "",
)

enum class PromptError { EMPTY }
