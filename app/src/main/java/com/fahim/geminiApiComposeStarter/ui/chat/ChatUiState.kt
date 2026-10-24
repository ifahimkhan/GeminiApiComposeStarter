package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class Participant {
    USER,
    MODEL,
}

@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Immutable UI state for the chat screen. */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }
