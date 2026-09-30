package com.fahim.geminiApiComposeStarter.ui.chat

/** One chat bubble. [id] is the Room primary key and is used as the LazyColumn key. */
data class ChatMessage(
    val id: Long,
    val text: String,
    val isUser: Boolean,
)

/** Single source of truth for the chat screen. Exposed as a StateFlow from ChatViewModel. */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

enum class PromptError { EMPTY }

enum class ThemeMode { SYSTEM, LIGHT, DARK }
