package com.example.geministarter.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class ContentPart(
    @SerialName("text") val text: String,
)

@Serializable
data class Content(
    @SerialName("parts") val parts: List<ContentPart>,
    @SerialName("role") val role: String? = null,
)

@Serializable
data class GenerateContentRequest(
    @SerialName("contents") val contents: List<Content>,
)

@Serializable
data class Candidate(
    @SerialName("content") val content: Content? = null,
    @SerialName("finishReason") val finishReason: String? = null,
)

@Serializable
data class GenerateContentResponse(
    @SerialName("candidates") val candidates: List<Candidate>? = null,
    @SerialName("error") val error: ApiErrorDetail? = null,
)

@Serializable
data class ApiErrorDetail(
    @SerialName("code") val code: Int? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("status") val status: String? = null,
)

sealed interface ProxyResponse {
    data class Success(val text: String) : ProxyResponse
    data class Error(val message: String) : ProxyResponse
}

interface GeminiProxyClient {
    suspend fun generateContent(
        prompt: String,
        apiKey: String,
        modelName: String = "gemini-3.6-flash",
    ): ProxyResponse
}

open class OkHttpGeminiProxyClient(
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1beta",
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build(),
) : GeminiProxyClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    override suspend fun generateContent(
        prompt: String,
        apiKey: String,
        modelName: String,
    ): ProxyResponse = withContext(Dispatchers.IO) {
        val requestBodyData = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(ContentPart(text = prompt))
                )
            )
        )

        val jsonString = json.encodeToString(requestBodyData)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonString.toRequestBody(mediaType)

        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = if (apiKey.isNotBlank()) {
            "$cleanBaseUrl/models/$modelName:generateContent?key=$apiKey"
        } else {
            "$cleanBaseUrl/models/$modelName:generateContent"
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("X-Goog-Api-Key", apiKey)
        }

        val request = requestBuilder.build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val responseBodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorDetail = runCatching {
                    json.decodeFromString<GenerateContentResponse>(responseBodyString).error
                }.getOrNull()

                val responseMsg = response.message
                val errorMessage = errorDetail?.message
                    ?: "HTTP ${response.code}: ${if (responseMsg.isBlank()) "Request failed" else responseMsg}"
                return@withContext ProxyResponse.Error(errorMessage)
            }

            val parsedResponse = json.decodeFromString<GenerateContentResponse>(responseBodyString)
            val replyText = parsedResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                .orEmpty()

            if (replyText.isBlank()) {
                ProxyResponse.Error("Empty response received from backend.")
            } else {
                ProxyResponse.Success(replyText)
            }
        } catch (e: IOException) {
            ProxyResponse.Error("Network error: ${e.localizedMessage ?: "Unable to connect"}")
        } catch (e: Exception) {
            ProxyResponse.Error("Error parsing response: ${e.localizedMessage ?: "Unexpected error"}")
        }
    }
}
