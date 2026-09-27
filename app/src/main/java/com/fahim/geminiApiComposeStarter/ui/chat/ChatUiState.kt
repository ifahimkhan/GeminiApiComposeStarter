package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val inputError: String? = null,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val hasApiKey: Boolean = true,
)

/** Ready-made prompts shown on the empty/welcome screen. */
val SUGGESTED_PROMPTS = listOf(
    "Explain something" to "Explain black holes like I'm a beginner",
    "Help me study" to "Quiz me on the French Revolution",
    "Write something" to "Write a short poem about the ocean at night",
    "Brainstorm ideas" to "Brainstorm names for a new mobile app",
)
