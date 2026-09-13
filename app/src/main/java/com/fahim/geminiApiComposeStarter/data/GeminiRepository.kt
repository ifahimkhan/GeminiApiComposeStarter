package com.fahim.geminiApiComposeStarter.data

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
    suspend fun generateConversation(messages: List<com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage>): Result<String> =
        generateText(messages.last().text)

    suspend fun generateConversation(
        messages: List<com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage>,
        attachments: List<com.fahim.geminiApiComposeStarter.ui.chat.PendingAttachment>,
    ): Result<String> = generateConversation(messages)

    suspend fun generateImage(prompt: String): Result<GeneratedImage> =
        Result.failure(UnsupportedOperationException("Image generation is not configured."))
}

data class GeneratedImage(val bytes: ByteArray, val mimeType: String)
