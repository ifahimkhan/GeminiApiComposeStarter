package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY createdAt DESC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT DISTINCT deckTitle FROM flashcards ORDER BY deckTitle ASC")
    fun getAllDeckTitles(): Flow<List<String>>

    @Query("SELECT * FROM flashcards WHERE deckTitle = :deckTitle ORDER BY createdAt ASC")
    fun getFlashcardsForDeck(deckTitle: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE deckTitle = :deckTitle ORDER BY createdAt ASC")
    suspend fun getFlashcardsForDeckSnapshot(deckTitle: String): List<FlashcardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Query("UPDATE flashcards SET difficulty = :rating, reviewCount = reviewCount + 1, lastReviewedAt = :timestamp WHERE id = :id")
    suspend fun recordReview(id: String, rating: String, timestamp: Long)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcard(id: String)

    @Query("DELETE FROM flashcards WHERE deckTitle = :deckTitle")
    suspend fun deleteDeck(deckTitle: String)

    @Query("DELETE FROM flashcards")
    suspend fun clearAll()
}
