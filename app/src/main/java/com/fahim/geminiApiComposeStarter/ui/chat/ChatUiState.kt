package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.AiProvider
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: Boolean = false,
    val errorMessage: String? = null,
    val selectedProvider: AiProvider = AiProvider.GEMINI,
    val availableProviders: Set<AiProvider> = emptySet(),
    val showModelSelector: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val hasApiKey: Boolean get() = selectedProvider in availableProviders
}
