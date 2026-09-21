package com.fahim.geminiApiComposeStarter.data

/** Abstraction over the Gemini call so the ViewModel can be unit tested. */
interface GeminiRepository {
    /** Sends [prompt], together with the picture stored at [imagePath] when there is one. */
    suspend fun generateText(prompt: String, imagePath: String? = null): Result<String>
}
