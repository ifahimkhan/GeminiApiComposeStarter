package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.Participant

/**
 * Immutable UI state for the conversational chat flow.
 * Hoisted in ChatViewModel and collected with collectAsStateWithLifecycle() in Compose.
 */
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "Student",
    val systemPromptTone: String = "Helpful AI Assistant",
) {
    /**
     * Backward-compatible property returning the most recent Gemini response.
     */
    val response: String
        get() = messages.lastOrNull { it.participant == Participant.GEMINI }?.text.orEmpty()
}

enum class PromptError {
    EMPTY
}
