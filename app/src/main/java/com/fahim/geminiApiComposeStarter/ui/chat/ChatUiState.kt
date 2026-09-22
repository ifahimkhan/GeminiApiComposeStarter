package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.db.ChatMessage

/** Immutable UI state for the full-history chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val isListening: Boolean = false,        // true while speech recognition is active
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "",               // from DataStore — personalises welcome text
)

enum class PromptError { EMPTY }
