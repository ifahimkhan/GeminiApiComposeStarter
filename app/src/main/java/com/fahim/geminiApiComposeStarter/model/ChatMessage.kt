package com.fahim.geminiApiComposeStarter.model

import java.util.UUID

enum class MessageRole { USER, GEMINI }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String = "default",
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSaved: Boolean = false,
    val imageUri: String? = null,
)

data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val mode: StudyMode = StudyMode.CHAT,
)

data class QuizAttempt(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val strongAreas: String = "",
    val revisionTopics: String = "",
)
