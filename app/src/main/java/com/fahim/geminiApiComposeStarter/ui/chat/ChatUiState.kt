package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class Participant {
    USER,
    ASSISTANT,
}

/**
 * Represents a single message bubble in the conversation.
 * IDs are stable and unique.
 */
@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Classifies errors shown in the UI.
 *
 * - [Retryable]: the last prompt can be resent (network timeout, 503, quota). Shows a Retry action.
 * - [NonRetryable]: retrying cannot help (missing speech services, invalid key, device error).
 *   Shown as a simple dismissible snackbar with no action.
 */
sealed class ChatError {
    abstract val message: String

    data class Retryable(override val message: String) : ChatError()
    data class NonRetryable(override val message: String) : ChatError()
}

/**
 * Immutable UI state for the conversational chat flow.
 */
@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false,
    val promptError: PromptError? = null,
    val chatError: ChatError? = null,
)

enum class PromptError { EMPTY }
