package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.preferences.UserPreferencesRepository
import com.fahim.geminiApiComposeStarter.model.ChatMessage

/** Immutable UI state for the chat screen. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val selectedModel: String = UserPreferencesRepository.DEFAULT_MODEL,
    val availableModels: List<String> = UserPreferencesRepository.AVAILABLE_MODELS,
    val hasApiKey: Boolean = true,
)

enum class PromptError { EMPTY }
