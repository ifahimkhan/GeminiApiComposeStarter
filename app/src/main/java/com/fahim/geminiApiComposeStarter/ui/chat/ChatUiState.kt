package com.fahim.geminiApiComposeStarter.ui.chat

/** Domain model representing a single chat bubble item with stable key. */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Immutable UI state for the multi-turn conversational chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }

