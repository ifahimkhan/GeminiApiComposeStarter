package com.fahim.geminiApiComposeStarter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY updatedAt DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE category = :category ORDER BY updatedAt DESC")
    fun getMemoriesByCategory(category: String): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchMemories(query: String): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY updatedAt DESC")
    suspend fun getMemoriesSnapshot(): List<UserMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<UserMemoryEntity>)

    @Update
    suspend fun updateMemory(memory: UserMemoryEntity)

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM user_memories WHERE content LIKE '%' || :keyword || '%'")
    suspend fun deleteMemoriesByKeyword(keyword: String): Int

    @Query("DELETE FROM user_memories")
    suspend fun clearAll()
}
