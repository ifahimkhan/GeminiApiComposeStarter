package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    /*
     * The API key is requested only when the Gemini model
     * needs to be created.
     *
     * The GenerativeModel is created lazily, so the key
     * is not accessed until generateText() is actually called.
     */
    private val model: GenerativeModel by lazy {

        val apiKey = apiKeyProvider()

        GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        )
    }

    override suspend fun generateText(
        prompt: String
    ): Result<String> = try {

        val response =
            model.generateContent(prompt)

        val text =
            response.text?.takeIf {
                it.isNotBlank()
            }

        if (text != null) {

            Result.success(text)

        } else {

            Result.failure(
                IllegalStateException(
                    "Empty response from Gemini"
                )
            )
        }

    } catch (e: CancellationException) {

        throw e

    } catch (e: Exception) {

        Log.e(
            TAG,
            "generateContent failed",
            e
        )

        Result.failure(e)
    }
}