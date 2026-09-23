package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.Participant

/**
 * Immutable UI state for the Gemini chat conversation flow.
 *
 * @property prompt Current text in the prompt input field.
 * @property messages Chronological list of chat messages exchanged between USER and MODEL.
 * @property isLoading True when waiting for Gemini to generate a response.
 * @property promptError Validation error (e.g. empty prompt submission).
 * @property errorMessage Transient error message to display in a Snackbar.
 * @property isDarkTheme Current active theme mode (true for dark, false for light).
 */
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val isDarkTheme: Boolean = false,
) {
    /**
     * Backward-compatible response property pointing to the latest model response.
     */
    val response: String
        get() = messages.lastOrNull { it.participant == Participant.MODEL }?.text.orEmpty()

    /**
     * Secondary constructor supporting legacy previews or direct response initialization.
     */
    constructor(
        prompt: String = "",
        response: String,
        isLoading: Boolean = false,
        promptError: PromptError? = null,
        errorMessage: String? = null,
        isDarkTheme: Boolean = false,
    ) : this(
        prompt = prompt,
        messages = if (response.isNotEmpty()) {
            listOf(ChatMessage(text = response, participant = Participant.MODEL))
        } else {
            emptyList()
        },
        isLoading = isLoading,
        promptError = promptError,
        errorMessage = errorMessage,
        isDarkTheme = isDarkTheme,
    )
}

enum class PromptError { EMPTY }
