package com.fahim.geminiApiComposeStarter.ui.chat

/** Represents a single message with an ID for stable LazyColumn keys */
data class ChatMessage(
    val id: Long = 0L,
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis()
)

enum class Participant { USER, MODEL }

data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
)

enum class PromptError { EMPTY }