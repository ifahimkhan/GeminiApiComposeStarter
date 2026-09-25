package com.example.assignment1_c061.data

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
}
