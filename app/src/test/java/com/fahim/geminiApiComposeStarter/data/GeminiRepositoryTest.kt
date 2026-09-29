package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.security.ApiKeyProvider
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
}
