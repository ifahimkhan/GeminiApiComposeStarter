package com.fahim.geminiApiComposeStarter.ui.chat


data class ChatUiState(
    /*
     * Current text inside the input field.
     *
     * This is deliberately separate from messages.
     */
    val inputText: String = "",

    /*
     * Actual conversation history.
     */
    val messages: List<ChatMessage> = emptyList(),

    /*
     * Network/API request state.
     */
    val isLoading: Boolean = false,

    /*
     * Validation error for empty input.
     */
    val promptError: PromptError? = null,

    /*
     * General API/repository error.
     */
    val errorMessage: String? = null,

    /*
     * Preferences.
     */
    val userName: String = "",
    val nameDraft: String = "",
    val showPreferences: Boolean = false,
    val isDarkMode: Boolean = false,

    /*
     * Currently selected image waiting to be sent.
     */
    val selectedImageUri: String? = null
)


data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val imageUri: String? = null
)


enum class PromptError {
    EMPTY
}