package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable

/** A single chat message in the PITWALL conversation. */
@Immutable
data class ChatMessage(
    val id: Long = System.nanoTime(),
    val sender: String, // "DRIVER" or "PITWALL"
    val text: String,
    val timestamp: String,
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

/** Immutable UI state for the PITWALL chat screen. */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val promptError: PromptError? = null,
)

enum class PromptError { EMPTY }
