package com.fahim.geminiApiComposeStarter.data

import android.content.Context

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val attachments: List<ChatAttachment> = emptyList(),
)

/** Abstraction over Gemini generative calls including conversation history and multimodal attachments. */
interface GeminiRepository {
    suspend fun generateChatResponse(
        context: Context,
        history: List<ChatMessage>,
        prompt: String,
        attachments: List<ChatAttachment> = emptyList(),
        modelName: String = "gemini-3.8-flash",
    ): Result<String>
}
