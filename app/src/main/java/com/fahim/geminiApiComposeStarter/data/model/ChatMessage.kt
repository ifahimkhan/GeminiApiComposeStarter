package com.fahim.geminiApiComposeStarter.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain model representing a chat message between the user and Gemini.
 */
data class ChatMessage(
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
