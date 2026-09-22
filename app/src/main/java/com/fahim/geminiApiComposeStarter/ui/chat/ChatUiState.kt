package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.preferences.AppTheme
import java.util.UUID

enum class Participant { USER, MODEL }

/** Single message in the conversation list with a stable identifier. */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Immutable UI state for the conversation flow. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val lastFailedPrompt: String? = null,
    val theme: AppTheme = AppTheme.SYSTEM,
    val isThemeDialogOpen: Boolean = false,
    val isClearHistoryDialogOpen: Boolean = false,
)

enum class PromptError { EMPTY }
