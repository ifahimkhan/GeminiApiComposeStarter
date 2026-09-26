package com.example.c210_utkarshtanna_mad_assignment1

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val message: String,
    val isUser: Boolean
)
