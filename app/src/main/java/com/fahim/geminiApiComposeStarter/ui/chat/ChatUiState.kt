package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.local.MessageParticipant

/** Representation of a single chat message for the UI with stable key (id). */
data class ChatMessageUiModel(
    val id: Long,
    val text: String,
    val participant: MessageParticipant,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
)

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatMessageUiModel> = emptyList(),
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val autoScroll: Boolean = true,
)

enum class PromptError { EMPTY }
