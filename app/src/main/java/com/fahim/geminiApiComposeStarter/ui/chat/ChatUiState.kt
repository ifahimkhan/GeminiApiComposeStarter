package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

/** Data model representing an individual chat message. */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
)

/** Immutable UI state for the single-screen prompt/response flow. */
data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }
