package com.example.c031_geminiapicompose.ui.chat

import com.example.c031_geminiapicompose.data.ChatMessage

/** Immutable UI state for the Gemini chat flow. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isDarkMode: Boolean? = null, // null follows system preference
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }
