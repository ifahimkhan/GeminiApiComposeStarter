package com.vaishnavi.n085.geminichat.ui.chat

import com.vaishnavi.n085.geminichat.data.local.Message

/** Immutable UI state for the chat screen, hoisted into a StateFlow. */
data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val userName: String = "Vaishnavi",
)

enum class PromptError { EMPTY }
