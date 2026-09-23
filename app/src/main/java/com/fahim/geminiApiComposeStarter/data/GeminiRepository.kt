package com.fahim.geminiApiComposeStarter.data

import android.graphics.Bitmap
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Abstraction over the Gemini API calls.
 * Supports basic generation, multi-turn conversational chat, multimodal vision requests,
 * and token-by-token response streaming.
 * Default interface methods ensure full backward compatibility with existing unit test fakes.
 */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>

    suspend fun generateChat(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        systemInstruction: String? = null,
    ): Result<String> = generateText(prompt)

    suspend fun generateMultimodal(
        prompt: String,
        imageBitmap: Bitmap,
        systemInstruction: String? = null,
    ): Result<String> = generateText(prompt)

    fun generateChatStream(
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        systemInstruction: String? = null,
    ): Flow<String> = flow {
        val res = generateChat(prompt, history, systemInstruction)
        res.onSuccess { emit(it) }
        res.onFailure { throw it }
    }
}
