package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage

/** Immutable screen state. Conversation data is restored from Room. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }
