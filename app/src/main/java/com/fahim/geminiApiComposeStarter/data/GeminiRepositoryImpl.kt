package com.fahim.geminiApiComposeStarter.data

import android.util.Log
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyProvider
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val secureApiKeyProvider: SecureApiKeyProvider,
    private val buildConfigApiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private var model: GenerativeModel? = null

    private suspend fun getModel(): GenerativeModel {
        model?.let { return it }

        val apiKey =
            secureApiKeyProvider.getApiKey(
                buildConfigApiKey
            )

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey
        ).also {
            model = it
        }
    }

    override suspend fun generateText(
        prompt: String
    ): Result<String> =
        withContext(Dispatchers.IO) {

            try {
                Log.d(
                    TAG,
                    "generateText started"
                )

                val response =
                    getModel()
                        .generateContent(prompt)

                Log.d(
                    TAG,
                    "generateContent returned"
                )

                val text =
                    response.text
                        ?.takeIf {
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

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "generateContent failed",
                    e
                )

                Result.failure(e)
            }
        }
}