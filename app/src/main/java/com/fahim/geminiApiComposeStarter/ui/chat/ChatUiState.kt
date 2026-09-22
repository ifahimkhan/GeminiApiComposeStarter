package com.fahim.geminiApiComposeStarter.ui.chat
import com.fahim.geminiApiComposeStarter.data.ThemePreference
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
)

enum class PromptError { EMPTY }