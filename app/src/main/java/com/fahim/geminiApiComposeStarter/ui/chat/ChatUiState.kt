package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.fahim.geminiApiComposeStarter.data.Conversation

/** Immutable UI state for the chat screen. */
@Immutable
data class ChatUiState(
    val prompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val errorMessage: String? = null,
    val conversations: List<Conversation> = emptyList(),
    val activeId: String = "",
    val isRestoring: Boolean = false,
    val openKeyboard: Boolean = true,
    val modelName: String = "gemini-3.6-flash",
    val hasSavedKey: Boolean = false,
    val settingsMessage: String? = null,
)

@Immutable
data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
)

@Stable
enum class ChatRole { USER, GEMINI }

@Stable
enum class PromptError { EMPTY }
