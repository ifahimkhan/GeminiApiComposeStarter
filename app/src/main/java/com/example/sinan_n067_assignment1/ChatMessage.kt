package com.example.sinan_n067_assignment1

import java.util.UUID

enum class Participant {
    USER,
    MODEL
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val participant: Participant,
    val timestamp: Long = System.currentTimeMillis()
)