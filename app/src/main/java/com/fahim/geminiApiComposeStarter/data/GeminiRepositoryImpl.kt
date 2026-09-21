package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "GeminiRepository"
// gemini-3.6-flash reasons for ~30s before replying, which exceeds the client's 80s socket timeout on slow links.
private const val DEFAULT_MODEL = "gemini-flash-lite-latest"
private const val DEFAULT_IMAGE_PROMPT = "Describe this image."

class GeminiRepositoryImpl(
    private val apiKeyProvider: suspend () -> String?,
    private val imageStore: ImageStore,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val modelLock = Mutex()
    private var model: GenerativeModel? = null

    // The key is decrypted only here, when the model is first created.
    private suspend fun model(): GenerativeModel? = modelLock.withLock {
        model ?: apiKeyProvider()?.let { GenerativeModel(modelName = modelName, apiKey = it) }
            .also { model = it }
    }

    override suspend fun generateText(prompt: String, imagePath: String?): Result<String> = try {
        val model = model() ?: return Result.failure(IllegalStateException("API key is not configured"))

        val response = if (imagePath == null) {
            model.generateContent(prompt)
        } else {
            val bitmap = imageStore.load(imagePath)
                ?: return Result.failure(IllegalStateException("The attached image could not be read"))
            model.generateContent(
                content {
                    image(bitmap)
                    text(prompt.ifBlank { DEFAULT_IMAGE_PROMPT })
                },
            )
        }

        val text = response.text?.takeIf { it.isNotBlank() }
        if (text != null) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Empty response from Gemini"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "generateContent failed", e)
        Result.failure(e)
    }
}
