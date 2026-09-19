package com.nmims.c010.gemini.data

import androidx.compose.runtime.Immutable
import java.util.UUID

enum class Author { USER, MODEL }

@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val author: Author,
    val timestamp: Long = System.currentTimeMillis()
)
