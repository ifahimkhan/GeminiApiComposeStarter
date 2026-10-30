package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode

/** Immutable UI state for the chat screen. */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isApiKeyConfigured: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val showClearHistoryDialog: Boolean = false,
    val showSettingsDialog: Boolean = false,
)

enum class PromptError { EMPTY }
