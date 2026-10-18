package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

/**
 * Immutable UI state for the conversational chat flow.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isDarkMode: Boolean? = null,
)

enum class PromptError { EMPTY }
