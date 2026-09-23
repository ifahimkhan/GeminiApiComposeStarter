package com.fahim.geminiApiComposeStarter.data

import android.graphics.Bitmap
import android.util.Log
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.model.MessageRole
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

private const val TAG = "GeminiRepository"
private const val DEFAULT_MODEL = "gemini-3.6-flash"

class GeminiRepositoryImpl(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
) : GeminiRepository {

    private val model = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey,
        tools = listOf(Tool.CODE_EXECUTION),
    )

    override suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        executeWithRetry {
            val response = model.generateContent(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        }
    }

    override suspend fun generateChat(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        executeWithRetry {
            val targetModel = if (!systemInstruction.isNullOrBlank()) {
                GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    systemInstruction = content { text(systemInstruction) },
                    tools = listOf(Tool.CODE_EXECUTION),
                )
            } else {
                model
            }

            val chatHistory = history.map { msg ->
                content(role = if (msg.role == MessageRole.USER) "user" else "model") {
                    text(msg.content)
                }
            }

            val chat = targetModel.startChat(history = chatHistory)
            val response = chat.sendMessage(prompt)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        }
    }

    override fun generateChatStream(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String?,
    ): Flow<String> = flow {
        val targetModel = if (!systemInstruction.isNullOrBlank()) {
            GenerativeModel(
                modelName = modelName,
                apiKey = apiKey,
                systemInstruction = content { text(systemInstruction) },
                tools = listOf(Tool.CODE_EXECUTION),
            )
        } else {
            model
        }

        val chatHistory = history.map { msg ->
            content(role = if (msg.role == MessageRole.USER) "user" else "model") {
                text(msg.content)
            }
        }

        val chat = targetModel.startChat(history = chatHistory)
        val responseFlow = chat.sendMessageStream(prompt)
        responseFlow.collect { chunk ->
            val chunkText = chunk.text
            if (!chunkText.isNullOrEmpty()) {
                emit(chunkText)
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun generateMultimodal(
        prompt: String,
        imageBitmap: Bitmap,
        systemInstruction: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        executeWithRetry {
            val targetModel = if (!systemInstruction.isNullOrBlank()) {
                GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    systemInstruction = content { text(systemInstruction) },
                    tools = listOf(Tool.CODE_EXECUTION),
                )
            } else {
                model
            }

            val inputContent = content {
                image(imageBitmap)
                if (prompt.isNotBlank()) {
                    text(prompt)
                }
            }

            val response = targetModel.generateContent(inputContent)
            val text = response.text?.takeIf { it.isNotBlank() }
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(IllegalStateException("Empty response from Gemini"))
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        maxRetries: Int = 1,
        block: suspend () -> Result<T>,
    ): Result<T> {
        var attempt = 0
        while (true) {
            try {
                val result = block()
                if (result.isSuccess || attempt >= maxRetries) {
                    return result
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= maxRetries) {
                    Log.e(TAG, "Request failed after $attempt retries", e)
                    return Result.failure(e)
                }
            }
            attempt++
            delay(500L * attempt)
        }
    }
}
