package com.example.c020_harsh_assignment1.data

import com.google.ai.client.generativeai.type.Content

/**
 * Abstraction over the Gemini text generation call
 * so the ViewModel can be unit tested.
 */
interface GeminiRepository {
    suspend fun generateText(
        prompt: String,
        history: List<Content> = emptyList()
    ): Result<String>
}
