package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

enum class ChatSender { USER, ASSISTANT }

data class ChatMessage(
    val id: Long = 0,
    val sender: ChatSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
)

/** Abstraction over the Gemini text generation call & message storage so ViewModel can be tested. */
interface GeminiRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(prompt: String): Result<String>
    suspend fun clearHistory()
    fun getThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(themeMode: ThemeMode)
}
