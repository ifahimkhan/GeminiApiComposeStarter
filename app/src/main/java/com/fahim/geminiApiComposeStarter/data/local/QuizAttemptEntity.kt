package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val percentage: Int,
    val timestamp: Long,
    val strongAreas: String,
    val revisionTopics: String,
)
