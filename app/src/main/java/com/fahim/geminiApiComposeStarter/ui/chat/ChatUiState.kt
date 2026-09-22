package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.preferences.AiTone

/** Immutable UI state for the conversational chat flow. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedTone: AiTone = AiTone.BALANCED,
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }
