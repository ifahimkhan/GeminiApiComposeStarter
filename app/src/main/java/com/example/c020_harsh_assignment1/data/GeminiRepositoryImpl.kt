package com.example.c020_harsh_assignment1.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    apiKey: String,
    modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
    )

    override suspend fun generateText(
        prompt: String,
        history: List<Content>,
    ): Result<String> {
        return try {
            val chat = model.startChat(history)
            val response = chat.sendMessage(prompt)

            val text = response.text?.trim()

            Log.d(TAG, "Gemini response received. Has text: ${!text.isNullOrEmpty()}")

            if (!text.isNullOrEmpty()) {
                Result.success(text)
            } else {
                Result.failure(
                    IllegalStateException(
                        "Gemini returned an empty response."
                    )
                )
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini request failed", e)

            Result.failure(
                IllegalStateException(
                    e.message ?: "Gemini request failed"
                )
            )
        }
    }
}
