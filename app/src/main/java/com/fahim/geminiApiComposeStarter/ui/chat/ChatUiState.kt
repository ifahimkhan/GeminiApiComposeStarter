package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable

/** Immutable UI state for the conversational chat flow. */
@Immutable
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }
