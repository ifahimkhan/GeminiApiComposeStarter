package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.model.ChatMessage

/**
 * Immutable UI state for the Gemini chat screen.
 *
 * @param prompt Current text entered by the user in the input bar.
 * @param messages Persistent list of conversation messages ordered chronologically.
 * @param isLoading Whether Gemini is currently generating a response.
 * @param promptError Validation error on the prompt input (e.g. empty submission).
 * @param errorMessage User-visible error banner or snackbar message.
 * @param canRetry Whether the user can retry the previous failed prompt.
 * @param lastFailedPrompt The prompt string that failed to generate, kept for retry.
 * @param hasApiKey Whether a valid API key was found or configured.
 * @param themeMode User selected theme ("SYSTEM", "LIGHT", "DARK").
 * @param userName Student or user display name.
 * @param showClearConfirmDialog Whether the clear chat history confirmation dialog is visible.
 * @param showStudentInfoDialog Whether the student viva/assignment info dialog is visible.
 */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val canRetry: Boolean = false,
    val lastFailedPrompt: String? = null,
    val hasApiKey: Boolean = true,
    val themeMode: String = "SYSTEM",
    val userName: String = "Tanmay Sharma",
    val showClearConfirmDialog: Boolean = false,
    val showStudentInfoDialog: Boolean = false,
)

enum class PromptError {
    EMPTY
}
