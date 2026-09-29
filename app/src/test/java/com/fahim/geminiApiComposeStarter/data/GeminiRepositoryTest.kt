package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.data.security.ApiKeyProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
