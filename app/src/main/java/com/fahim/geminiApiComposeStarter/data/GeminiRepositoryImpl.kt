package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(modelName = modelName, apiKey = apiKey)
    private var chatSession: Chat = model.startChat()

    override fun sendMessageStream(prompt: String): Flow<String> = flow {
        try {
            val responseStream = chatSession.sendMessageStream(prompt)
            responseStream.collect { chunk ->
                chunk.text?.let { textChunk ->
                    if (textChunk.isNotEmpty()) {
                        emit(textChunk)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "sendMessageStream failed", e)
            val cleanMessage = when {
                e.message?.contains("503") == true -> "Server is temporarily busy. Please retry in a moment."
                e.message?.contains("404") == true -> "Model endpoint not found."
                else -> e.localizedMessage ?: "Unexpected error occurred."
            }
            throw Exception(cleanMessage)
        }
    }

    override fun resetChat() {
        chatSession = model.startChat()
    }
}