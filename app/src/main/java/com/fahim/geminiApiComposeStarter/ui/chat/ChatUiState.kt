package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

/** Immutable UI state for Gemini Orbit conversational interface. */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val response: String = "", // legacy backward-compatibility support
    val isLoading: Boolean = false,
    val isListeningVoice: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

enum class PromptError { EMPTY }
