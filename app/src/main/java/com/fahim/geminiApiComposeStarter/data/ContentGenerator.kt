package com.fahim.geminiApiComposeStarter.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content

/**
 * Testable seam around Google's Generative AI SDK (Phase 5c).
 * Decouples GeminiRepository from direct GenerativeModel instantiation, allowing
 * deterministic unit testing of retries, fallback switching, and error mapping on the JVM.
 */
interface ContentGenerator {
    suspend fun generateContent(
        modelName: String,
        apiKey: String,
        prompt: String,
        history: List<Content>,
    ): String
}

/**
 * Production implementation backed by Google's [GenerativeModel].
 */
class DefaultContentGenerator : ContentGenerator {
    @Volatile
    private var primaryModel: GenerativeModel? = null
    @Volatile
    private var fallbackModel: GenerativeModel? = null
    @Volatile
    private var cachedApiKey: String? = null

    override suspend fun generateContent(
        modelName: String,
        apiKey: String,
        prompt: String,
        history: List<Content>,
    ): String {
        val model = getOrCreateModel(modelName, apiKey)
        val response = if (history.isEmpty()) {
            model.generateContent(prompt)
        } else {
            val chat = model.startChat(history)
            chat.sendMessage(prompt)
        }
        return response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Received empty response from Gemini.")
    }

    private fun getOrCreateModel(modelName: String, apiKey: String): GenerativeModel {
        if (cachedApiKey != apiKey) {
            synchronized(this) {
                if (cachedApiKey != apiKey) {
                    cachedApiKey = apiKey
                    primaryModel = null
                    fallbackModel = null
                }
            }
        }
        if (primaryModel?.modelName == modelName) {
            return primaryModel!!
        }
        if (fallbackModel?.modelName == modelName) {
            return fallbackModel!!
        }
        return synchronized(this) {
            if (cachedApiKey != apiKey) {
                cachedApiKey = apiKey
                primaryModel = null
                fallbackModel = null
            }
            if (primaryModel?.modelName == modelName) {
                primaryModel!!
            } else if (fallbackModel?.modelName == modelName) {
                fallbackModel!!
            } else {
                val created = GenerativeModel(modelName = modelName, apiKey = apiKey)
                if (primaryModel == null) {
                    primaryModel = created
                } else {
                    fallbackModel = created
                }
                created
            }
        }
    }
}
