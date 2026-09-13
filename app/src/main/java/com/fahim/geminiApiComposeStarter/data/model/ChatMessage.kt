package com.fahim.geminiApiComposeStarter.data.model

import androidx.compose.runtime.Immutable

/** Who produced a turn in the conversation. */
enum class Author { USER, MODEL }

/**
 * One turn of the conversation. Marked [Immutable] so Compose can skip recomposing a bubble
 * whose message instance has not changed.
 */
@Immutable
data class ChatMessage(
    val id: Long,
    val text: String,
    val author: Author,
    val createdAt: Long,
)
