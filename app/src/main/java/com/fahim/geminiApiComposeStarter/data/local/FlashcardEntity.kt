package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class FlashcardRating {
    AGAIN,
    HARD,
    GOOD,
    EASY,
}

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckTitle: String,
    val front: String,
    val back: String,
    val difficulty: String = FlashcardRating.GOOD.name,
    val reviewCount: Int = 0,
    val lastReviewedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
)
