package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.security.ApiKeyProvider
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeApiKeyProvider(private val key: String?) : ApiKeyProvider {
        override suspend fun getApiKey(): String? = key
    }

    @Test
    fun generateText_missingApiKey_returnsFailure() = runTest(testDispatcher) {
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider(null),
            ioDispatcher = testDispatcher,
        )

        val result = repo.generateText("Hello")
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("missing", ignoreCase = true))
    }

    @Test
    fun generateText_blankApiKey_returnsFailure() = runTest(testDispatcher) {
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider("   "),
            ioDispatcher = testDispatcher,
        )

        val result = repo.generateText("Hello")
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("missing", ignoreCase = true))
    }

    @Test
    fun mapException_unknownHost_mapsToNetworkError() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(UnknownHostException("Unable to resolve host"))
        assertTrue(mapped is IOException)
        assertTrue(mapped.message?.contains("Network connection error", ignoreCase = true) == true)
    }

    @Test
    fun mapException_socketTimeout_mapsToNetworkError() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(SocketTimeoutException("timeout"))
        assertTrue(mapped is IOException)
        assertTrue(mapped.message?.contains("Network connection error", ignoreCase = true) == true)
    }

    @Test
    fun mapException_apiKeyInvalid_mapsToInvalidCredentials() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(RuntimeException("API_KEY_INVALID: Key not valid"))
        assertTrue(mapped is IllegalArgumentException)
        assertTrue(mapped.message?.contains("Invalid API key credentials", ignoreCase = true) == true)
    }

    @Test
    fun mapException_permissionDenied_mapsToSecurityException() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(RuntimeException("PERMISSION_DENIED: 403"))
        assertTrue(mapped is SecurityException)
        assertTrue(mapped.message?.contains("Authentication or permission denied", ignoreCase = true) == true)
    }

    @Test
    fun mapException_rateLimit429_mapsToQuotaExceeded() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(RuntimeException("RESOURCE_EXHAUSTED: 429 quota exceeded"))
        assertTrue(mapped is IllegalStateException)
        assertTrue(mapped.message?.contains("Rate limit or quota exceeded", ignoreCase = true) == true)
    }

    @Test
    fun mapException_modelNotFound404_mapsToUnavailable() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(RuntimeException("NOT_FOUND: 404 model not found"))
        assertTrue(mapped is IllegalStateException)
        assertTrue(mapped.message?.contains("currently unavailable", ignoreCase = true) == true)
    }

    @Test
    fun mapException_unknownError_mapsToUnexpectedError() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val mapped = repo.mapExceptionToUserFriendlyError(RuntimeException("Something custom"))
        assertEquals("Something custom", mapped.message)
    }

    @Test
    fun buildContentHistory_emptyList_returnsEmpty() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val result = repo.buildContentHistory(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun buildContentHistory_userAndAssistantTurns_mapsRolesCorrectly() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val history = listOf(
            ChatMessage(text = "What is 2 + 2?", participant = Participant.USER),
            ChatMessage(text = "2 + 2 = 4", participant = Participant.ASSISTANT),
        )
        val result = repo.buildContentHistory(history)
        assertEquals(2, result.size)
        assertEquals("user", result[0].role)
        assertEquals("What is 2 + 2?", (result[0].parts.first() as TextPart).text)
        assertEquals("model", result[1].role)
        assertEquals("2 + 2 = 4", (result[1].parts.first() as TextPart).text)
    }

    @Test
    fun buildContentHistory_startsWithAssistant_dropsInitialAssistantTurn() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val history = listOf(
            ChatMessage(text = "Welcome to Gemini!", participant = Participant.ASSISTANT),
            ChatMessage(text = "Hi", participant = Participant.USER),
            ChatMessage(text = "Hello!", participant = Participant.ASSISTANT),
        )
        val result = repo.buildContentHistory(history)
        assertEquals(2, result.size)
        assertEquals("user", result[0].role)
        assertEquals("model", result[1].role)
    }

    @Test
    fun buildContentHistory_endsInUserTurn_dropsTrailingUserTurn() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))
        val history = listOf(
            ChatMessage(text = "What is 2 + 2?", participant = Participant.USER),
            ChatMessage(text = "2 + 2 = 4", participant = Participant.ASSISTANT),
            ChatMessage(text = "Unanswered user message", participant = Participant.USER),
        )
        val result = repo.buildContentHistory(history)
        assertEquals(2, result.size)
        assertEquals("model", result.last().role)
    }
}
