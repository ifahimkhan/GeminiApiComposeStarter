package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

/** Immutable UI state for the Gemini chat experience. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedModel: String = "gemini-3.6-flash",
)

enum class PromptError { EMPTY }
