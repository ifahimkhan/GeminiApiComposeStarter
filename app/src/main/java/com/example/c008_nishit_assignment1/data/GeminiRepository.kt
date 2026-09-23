package com.example.c008_nishit_assignment1.data

/**
 * Interface abstraction for interacting with Gemini AI service.
 */
interface GeminiRepository {
    suspend fun generateResponse(prompt: String): Result<String>
}
