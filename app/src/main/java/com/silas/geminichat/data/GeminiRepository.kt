package com.silas.geminichat.data

/**
 * Abstraction over the Gemini text-generation call so the ViewModel can be
 * unit-tested against a fake implementation.
 *
 * @author Silas · n091 (MBA Tech CE, E1)
 */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>

    companion object {
        /** Free-tier Gemini model used as the default. */
        const val DEFAULT_MODEL = "gemini-3.6-flash"
    }
}
