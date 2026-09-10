package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

/** Immutable UI state for the multi-turn chat flow. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
)
