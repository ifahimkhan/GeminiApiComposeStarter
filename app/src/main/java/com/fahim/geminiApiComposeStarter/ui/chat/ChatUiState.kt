package com.fahim.geminiApiComposeStarter.ui.chat

import java.util.UUID

/**
 * Distinguishes participants in the chat conversation.
 */
enum class Participant {
    USER,
    ASSISTANT,
}

/**
 * Immutable chat message model with explicit stable ID.
 * Stores strictly conversational content; never contains credentials or sensitive session keys.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val isPending: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val isFromUser: Boolean get() = participant == Participant.USER
}

/**
 * Immutable UI state for the conversational chat flow.
 */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val response: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }

