package com.fahim.geminiApiComposeStarter.data

enum class Author { USER, GEMINI }

/** One chat bubble. [id] is stable and unique, so it doubles as the LazyColumn key. */
data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
)
