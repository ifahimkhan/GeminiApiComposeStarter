package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeakTopicDao {
    @Query("SELECT * FROM weak_topics ORDER BY failureCount DESC, lastPracticedAt DESC")
    fun getAllWeakTopics(): Flow<List<WeakTopicEntity>>

    @Query("SELECT * FROM weak_topics WHERE topic = :topic LIMIT 1")
    suspend fun findByTopic(topic: String): WeakTopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakTopic(topic: WeakTopicEntity)

    @Update
    suspend fun updateWeakTopic(topic: WeakTopicEntity)

    @Query("DELETE FROM weak_topics WHERE id = :id")
    suspend fun deleteWeakTopic(id: String)

    @Query("DELETE FROM weak_topics")
    suspend fun clearAll()
}
