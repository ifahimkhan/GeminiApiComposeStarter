package com.vaishnavi.n085.geminichat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao {

    @Query("SELECT * FROM messages ORDER BY id ASC")
    suspend fun getAll(): List<Message>

    @Insert
    suspend fun insert(message: Message)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
