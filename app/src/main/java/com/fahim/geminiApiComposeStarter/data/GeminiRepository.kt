package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    fun sendMessageStream(prompt: String): Flow<String>
    fun resetChat()
}