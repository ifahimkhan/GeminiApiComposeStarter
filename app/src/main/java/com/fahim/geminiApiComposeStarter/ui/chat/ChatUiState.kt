package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme

/** Immutable UI state for the chat interface. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val hasApiKey: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
) {
    /** Backward compatibility accessor for single response */
    val response: String
        get() = messages.lastOrNull { it.participant == Participant.GEMINI }?.text.orEmpty()
}

enum class PromptError { EMPTY }
