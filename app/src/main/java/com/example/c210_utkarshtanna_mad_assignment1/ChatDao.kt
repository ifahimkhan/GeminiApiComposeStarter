package com.example.c210_utkarshtanna_mad_assignment1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao {
    @Insert
    suspend fun insert(chat: ChatEntity)

    @Query("SELECT * FROM ChatEntity")
    suspend fun getAllChats(): List<ChatEntity>
}
