package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

enum class Participant {
    USER,
    ASSISTANT,
}

/**
 * Represents a single message bubble in the conversation.
 * IDs are stable and unique.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Immutable UI state for the conversational chat flow.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val response: String = "",
)

enum class PromptError { EMPTY }
