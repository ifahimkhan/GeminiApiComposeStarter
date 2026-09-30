package com.fahim.geminiApiComposeStarter.ui.chat

/** Immutable UI state for the single-screen prompt/response flow. */
data class ChatUiState(
    val prompt: String = "",
    val response: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isApiKeyConfigured: Boolean = false,
    val showApiKeyDialog: Boolean = false,
)

enum class PromptError {
    EMPTY,
    TOO_LONG,
}
