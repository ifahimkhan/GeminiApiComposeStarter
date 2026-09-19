package com.fahim.geminiApiComposeStarter.data

const val MISSING_API_KEY_MESSAGE =
    "GEMINI_API_KEY is missing. Add it to local.properties and rebuild."

/** Abstraction over the Gemini text generation call so the ViewModel can be unit tested. */
interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
}
