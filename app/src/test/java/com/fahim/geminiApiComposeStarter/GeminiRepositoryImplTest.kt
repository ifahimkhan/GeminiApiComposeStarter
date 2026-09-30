package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepositoryImpl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRepositoryImplTest {

    @Test
    fun apiKeyProviderIsNotInvokedOnInitialization() {
        var providerCallCount = 0
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = {
                providerCallCount++
                "test-dummy-key"
            },
            contentGenerator = { _, _ -> "test response" }
        )

        // API key provider must not be invoked during construction
        assertEquals(0, providerCallCount)
    }

    @Test
    fun apiKeyProviderIsInvokedLazilyWhenGenerateTextIsCalled() = runTest {
        var providerCallCount = 0
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = {
                providerCallCount++
                "test-dummy-key"
            },
            contentGenerator = { _, _ -> "test response" }
        )

        assertEquals(0, providerCallCount)

        val result = repository.generateText("Hello")

        assertEquals(1, providerCallCount)
        assertTrue(result.isSuccess)
        assertEquals("test response", result.getOrNull())

        // Second call should reuse cached GenerativeModel without invoking provider again
        repository.generateText("Hello again")
        assertEquals(1, providerCallCount)
    }

    @Test
    fun successfulResponseIsReturned() = runTest {
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = { "test-dummy-key" },
            contentGenerator = { _, prompt -> "Processed response for: $prompt" }
        )

        val result = repository.generateText("Summarize this document")

        assertTrue(result.isSuccess)
        assertEquals("Processed response for: Summarize this document", result.getOrNull())
    }

    @Test
    fun emptyOrBlankResponseIsConvertedToFailure() = runTest {
        val repositoryWithEmpty = GeminiRepositoryImpl(
            apiKeyProvider = { "test-dummy-key" },
            contentGenerator = { _, _ -> "" }
        )
        val emptyResult = repositoryWithEmpty.generateText("prompt")
        assertTrue(emptyResult.isFailure)
        assertEquals("Empty response from Gemini", emptyResult.exceptionOrNull()?.message)

        val repositoryWithBlank = GeminiRepositoryImpl(
            apiKeyProvider = { "test-dummy-key" },
            contentGenerator = { _, _ -> "   \n\t  " }
        )
        val blankResult = repositoryWithBlank.generateText("prompt")
        assertTrue(blankResult.isFailure)
        assertEquals("Empty response from Gemini", blankResult.exceptionOrNull()?.message)
    }

    @Test
    fun repositoryFailureIsConvertedToExpectedResultFailure() = runTest {
        val expectedException = RuntimeException("API quota exceeded")
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = { "test-dummy-key" },
            contentGenerator = { _, _ -> throw expectedException }
        )

        val result = repository.generateText("prompt")

        assertTrue(result.isFailure)
        assertEquals("API quota exceeded", result.exceptionOrNull()?.message)
    }

    @Test(expected = CancellationException::class)
    fun cancellationExceptionIsRethrownAndNotSwallowed() = runTest {
        val repository = GeminiRepositoryImpl(
            apiKeyProvider = { "test-dummy-key" },
            contentGenerator = { _, _ -> throw CancellationException("Job was cancelled") }
        )

        repository.generateText("prompt")
    }

    @Test
    fun missingApiKeyProducesFailure() = runTest {
        val repositoryWithNullKey = GeminiRepositoryImpl(
            apiKeyProvider = { null },
            contentGenerator = { _, _ -> "Should not reach here" }
        )
        val nullResult = repositoryWithNullKey.generateText("prompt")
        assertTrue(nullResult.isFailure)
        assertTrue(
            nullResult.exceptionOrNull()?.message?.contains("Gemini API key is not available") == true
        )

        val repositoryWithBlankKey = GeminiRepositoryImpl(
            apiKeyProvider = { "   " },
            contentGenerator = { _, _ -> "Should not reach here" }
        )
        val blankResult = repositoryWithBlankKey.generateText("prompt")
        assertTrue(blankResult.isFailure)
        assertTrue(
            blankResult.exceptionOrNull()?.message?.contains("Gemini API key is not available") == true
        )
    }
}
