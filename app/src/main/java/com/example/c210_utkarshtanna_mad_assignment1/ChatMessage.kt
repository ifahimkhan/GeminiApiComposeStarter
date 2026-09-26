package com.example.c210_utkarshtanna_mad_assignment1

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val id: Long = System.currentTimeMillis()
)
