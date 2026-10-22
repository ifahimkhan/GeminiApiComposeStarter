package com.example.geminiapicomposestarter.data

import com.example.geminiapicomposestarter.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class GeminiRepository(
    private val apiKeyProvider: suspend () -> String? = { BuildConfig.GEMINI_API_KEY }
) {
    companion object {
        const val MODEL_NAME = "gemini-1.5-flash"
    }

    private var generativeModel: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel {
        if (generativeModel == null) {
            val key = apiKeyProvider()?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
            generativeModel = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = key
            )
        }
        return generativeModel!!
    }

    open suspend fun sendMessage(history: List<ChatMessage>, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val key = apiKeyProvider()?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
            if (key.isBlank() || key == "your_gemini_api_key_here") {
                // Mock response if no live key is configured yet
                return@withContext Result.success(getSimulatedResponse(prompt))
            }

            val model = getModel()

            // Prepare multi-turn history content
            val pastContent = history.map { message ->
                val role = if (message.participant == Participant.USER) "user" else "model"
                content(role = role) {
                    text(message.text)
                }
            }

            val chat = model.startChat(history = pastContent)
            val response = chat.sendMessage(prompt)
            val responseText = response.text ?: "I am sorry, I couldn't generate a response."
            Result.success(responseText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getSimulatedResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") ->
                "Hello Gatha! It's nice to meet you. How can I help you today?"
            lower.contains("founder of google") ->
                "Hello! Google was founded by Larry Page and Sergey Brin.\n\nThey created the search engine in 1998 while they were Ph.D. students at Stanford University."
            lower.contains("ceo of google") ->
                "The CEO of Google is Sundar Pichai. He has been serving as the CEO of Google since 2015 and also became the CEO of its parent company, Alphabet Inc., in 2019."
            lower.contains("lazycolumn") ->
                "# What is the Primary Role of LazyColumn?\n\nIn Jetpack Compose, LazyColumn is a vertically scrolling list that only composes and lays out the items that are currently visible on the screen. It is the Compose equivalent of RecyclerView in traditional Android Views, providing high performance and efficient memory management."
            else ->
                "Thank you for your prompt! This is a multi-turn response from Gemini with full conversational context."
        }
    }
}
