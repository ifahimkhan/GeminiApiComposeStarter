package com.fahim.geminiApiComposeStarter.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private const val DEFAULT_MODEL =
    "gemini-3.6-flash"


class GeminiRepositoryImpl(
    private val context: Context,
    private val secureApiKeyStore: SecureApiKeyStore
) : GeminiRepository {


    override suspend fun initialize() {

        secureApiKeyStore.initializeEncryptedKey()
    }


    override suspend fun sendMessage(
        prompt: String,
        imageUri: Uri?
    ): String =
        withContext(Dispatchers.IO) {

            /*
             * Retrieve the decrypted API key.
             */
            val apiKey =
                secureApiKeyStore.getDecryptedApiKey()


            require(apiKey.isNotBlank()) {
                "API key is empty."
            }


            /*
             * Create the Gemini model.
             */
            val model =
                GenerativeModel(
                    modelName = DEFAULT_MODEL,
                    apiKey = apiKey
                )


            /*
             * TEXT ONLY
             *
             * Keep the existing working path unchanged.
             */
            if (imageUri == null) {

                val response =
                    model.generateContent(
                        prompt
                    )

                return@withContext(
                        response.text
                            ?: "Gemini returned no text."
                        )
            }


            /*
             * IMAGE + TEXT
             *
             * Read the selected image from the
             * Android content resolver.
             */
            val bitmap =
                context.contentResolver
                    .openInputStream(imageUri)
                    ?.use { inputStream ->

                        BitmapFactory.decodeStream(
                            inputStream
                        )
                    }
                    ?: throw IllegalStateException(
                        "Unable to read the selected image."
                    )


            /*
             * Construct multimodal content.
             */
            val inputContent =
                content {

                    image(bitmap)

                    text(prompt)
                }


            /*
             * Send image + prompt to Gemini.
             */
            val response =
                model.generateContent(
                    inputContent
                )


            response.text
                ?: "Gemini returned no text."
        }
}