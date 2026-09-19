package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.8-flash"

class GeminiRepositoryImpl(
    apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Using v1 API version for better compatibility with 2026 models
    private val requestOptions = RequestOptions(apiVersion = "v1")
    
    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        requestOptions = requestOptions
    )

    override suspend fun generateText(
        prompt: String,
        history: List<HistoryItem>
    ): Result<String> = try {
        val chatHistory = history.map { item ->
            content(role = if (item.isFromUser) "user" else "model") { text(item.text) }
        }
        val chat = model.startChat(history = chatHistory)
        val response = chat.sendMessage(prompt)
        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        val stackTrace = e.stackTraceToString()
        Log.e(TAG, "generateContent failed: ${e.message}\n$stackTrace")
        
        // Handle 404, Serialization errors, and other API failures gracefully
        val errorMessage = when {
            stackTrace.contains("SerializationException") || 
            stackTrace.contains("missing key 'details'") -> 
                "API error: Unexpected response format from '$modelName'. Try 'gemini-1.5-flash-latest' or check API settings."
            e.message?.contains("404") == true -> 
                "API error: Model '$modelName' not found on endpoint '${requestOptions.apiVersion}'. Please verify model availability."
            else -> e.message ?: "Something went wrong"
        }
        Result.failure(Exception(errorMessage))
    }
}
