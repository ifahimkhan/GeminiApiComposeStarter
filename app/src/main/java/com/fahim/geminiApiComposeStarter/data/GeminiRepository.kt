package com.fahim.geminiApiComposeStarter.data

interface GeminiRepository {
    suspend fun generateText(prompt: String): Result<String>
}
