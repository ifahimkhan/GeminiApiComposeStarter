package com.n149.geminichat.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reading and writing user text feedback on Gemini responses.
 */
@Dao
interface FeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    /** Live feedback for a single bubble — used to show the saved note under it. */
    @Query("SELECT * FROM feedback WHERE messageId = :messageId LIMIT 1")
    fun getFeedbackForMessage(messageId: Long): Flow<FeedbackEntity?>

    /** One-shot version used by the ViewModel for personalisation. */
    @Query("SELECT * FROM feedback WHERE messageId = :messageId LIMIT 1")
    suspend fun getFeedbackForMessageOnce(messageId: Long): FeedbackEntity?

    /** All feedback ordered newest-first, used by PersonalisationEngine. */
    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<FeedbackEntity>>

    /** Recent feedback texts for system-prompt building. */
    @Query("SELECT userText FROM feedback ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFeedbackTexts(limit: Int = 10): List<String>

    @Query("DELETE FROM feedback WHERE messageId = :messageId")
    suspend fun deleteFeedbackForMessage(messageId: Long)

    @Query("DELETE FROM feedback")
    suspend fun clearAll()
}
