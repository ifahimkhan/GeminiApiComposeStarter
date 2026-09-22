package com.fahim.geminiApiComposeStarter.data

/** Who produced a line of the conversation. */
enum class Author { USER, GEMINI }

/**
 * One turn of the conversation as the UI renders it. [id] is the Room row id and is used as the
 * stable key for the chat `LazyColumn`, so bubbles are never recomposed into the wrong slot.
 */
data class ChatMessage(
    val id: Long = 0L,
    val text: String,
    val author: Author,
    val timestamp: Long = System.currentTimeMillis(),
)
