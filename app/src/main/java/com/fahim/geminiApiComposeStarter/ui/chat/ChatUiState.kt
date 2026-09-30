package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.db.ChatMessageEntity

/** Immutable UI state for the chat screen flow. */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }
