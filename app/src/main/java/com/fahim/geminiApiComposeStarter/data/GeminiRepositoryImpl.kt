package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.FinishReason
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CancellationException

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class SafetyBlockedException(message: String) : Exception(message)
class MissingApiKeyException : Exception("No Gemini API key configured.")

class GeminiRepositoryImpl(
    private val apiKeyProvider: () -> String?,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    // Primary constructor overload for backward compatibility
    constructor(apiKey: String, modelName: String = DEFAULT_MODEL) : this(
        apiKeyProvider = { apiKey },
        modelName = modelName
    )

    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
    )

    private val config = generationConfig {
        temperature = 0.7f
        maxOutputTokens = 2048
    }

    private fun getModel(apiKey: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings,
        )
    }

    override suspend fun generateText(prompt: String): Result<String> = try {
        val apiKey = apiKeyProvider()?.takeIf { it.isNotBlank() }
            ?: throw MissingApiKeyException()

        val model = getModel(apiKey)
        val response = model.generateContent(prompt)

        // Check if content was blocked due to safety
        val candidate = response.candidates.firstOrNull()
        if (candidate?.finishReason == FinishReason.SAFETY) {
            throw SafetyBlockedException("Response was blocked by content safety filters.")
        }

        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response received from the model."))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Guard logging to avoid leaking sensitive information to system logs in production
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "generateContent failed: ${e.javaClass.simpleName} - ${e.message?.take(100)}")
        }
        Result.failure(e)
    }
}
