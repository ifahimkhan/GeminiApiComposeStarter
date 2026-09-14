package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    fun generateTextStream(prompt: String): Flow<String>
    fun generateConversationStream(messages: List<com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage>): Flow<String> =
        generateTextStream(messages.last().text)

    fun generateConversationStream(
        messages: List<com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage>,
        attachments: List<com.fahim.geminiApiComposeStarter.ui.chat.PendingAttachment>,
    ): Flow<String> = generateConversationStream(messages)

    suspend fun generateImage(prompt: String): Result<GeneratedImage> =
        Result.failure(UnsupportedOperationException("Image generation is not configured."))
}

data class GeneratedImage(val bytes: ByteArray, val mimeType: String)
