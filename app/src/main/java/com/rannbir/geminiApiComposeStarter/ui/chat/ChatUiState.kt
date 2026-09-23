package com.rannbir.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.rannbir.geminiApiComposeStarter.data.local.ChatMessageEntity

enum class PromptError { EMPTY }

/**
 * Immutable UI state for the Gemini chat experience.
 * Fully hoisted and exposed via StateFlow.
 */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessageEntity> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userDisplayName: String = "Rannbir Sachdeva",
    val rollNumber: String = "N087",
)
