package com.n149.geminichat.ui.chat

import com.n149.geminichat.data.FeedbackEntity
import com.n149.geminichat.data.MessageEntity

/**
 * Summary of a chat conversation for the slidebar history list.
 */
data class ChatSessionItem(
    val sessionId: String,
    val title: String,
    val timestamp: Long,
    val messageCount: Int
)

/**
 * Complete UI state for the chat screen.
 * Collected as a StateFlow; composables are fully stateless.
 */
data class ChatUiState(
    /** All persisted messages for the active session, displayed in order */
    val messages: List<MessageEntity> = emptyList(),

    /** List of all conversation sessions for the slidebar history */
    val sessions: List<ChatSessionItem> = emptyList(),

    /** Currently active conversation ID */
    val currentSessionId: String = "default",

    /**
     * Map of messageId → FeedbackEntity.
     * Each Gemini bubble checks this to show/edit its saved feedback note.
     */
    val feedbackMap: Map<Long, FeedbackEntity> = emptyMap(),

    /** True while waiting for Gemini to respond */
    val isLoading: Boolean = false,

    /** Non-null on API error — shown as a Snackbar */
    val errorMessage: String? = null,

    /** Current text in the bottom input field */
    val inputText: String = "",

    /** True while the voice recognizer is recording */
    val isListening: Boolean = false,

    /**
     * Topic label detected by PersonalisationEngine from recent questions.
     * Displayed in the top-bar subtitle (e.g. "💻 Code", "🔬 Science").
     */
    val detectedTopic: String = "✦ General",

    /**
     * Non-null when the inline feedback editor is open.
     * Holds the messageId of the Gemini response being reviewed.
     */
    val pendingFeedbackMessageId: Long? = null
)
