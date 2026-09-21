package com.fahim.geminiApiComposeStarter.data

enum class Author { USER, GEMINI }

/**
 * One chat bubble. [id] is stable and unique, so it doubles as the LazyColumn key.
 * [imagePath] is a file in app storage when the user attached a picture.
 */
data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
    val imagePath: String? = null,
)
