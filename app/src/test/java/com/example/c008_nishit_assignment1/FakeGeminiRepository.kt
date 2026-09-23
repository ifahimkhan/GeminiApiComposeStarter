package com.example.c008_nishit_assignment1

import com.example.c008_nishit_assignment1.data.GeminiRepository

/**
 * Fake GeminiRepository implementation for unit testing ChatViewModel behavior.
 */
class FakeGeminiRepository : GeminiRepository {
    var shouldReturnError = false
    var errorToReturn: Throwable = Exception("API error")
    var responseToReturn: String = "Hello! I am Gemini AI."

    override suspend fun generateResponse(prompt: String): Result<String> {
        return if (shouldReturnError) {
            Result.failure(errorToReturn)
        } else {
            Result.success(responseToReturn)
        }
    }
}
