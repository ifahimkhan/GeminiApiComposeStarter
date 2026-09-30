package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.security.InMemoryKeyProvider
import com.fahim.geminiApiComposeStarter.security.SecureApiKeyManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRepositoryImplTest {

    @Test
    fun generateText_whenApiKeyMissing_returnsFailure() = runTest {
        // Manager with no key configured
        val emptyKeyManager = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
        )

        val repository = GeminiRepositoryImpl(apiKeyManager = emptyKeyManager)
        val result = repository.generateText("Hello")

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is Exception)
        assertTrue(exception?.message?.contains("not configured", ignoreCase = true) == true)
    }

    @Test
    fun generateText_whenKeyContainsSensitiveInfo_sanitizesErrorMessage() = runTest {
        val dummyKey = "AIzaSyTestValidLengthKey123456789"
        val manager = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
        ).apply {
            storeApiKey(dummyKey)
        }

        // Repository with modelFactory that throws an exception simulating an HTTP error with a sensitive URL
        val repository = GeminiRepositoryImpl(
            apiKeyManager = manager,
            modelFactory = { _, _ ->
                throw RuntimeException("HTTP 400 at https://generativelanguage.googleapis.com/v1beta/models?key=$dummyKey")
            },
        )

        val result = repository.generateText("Test prompt")
        assertTrue(result.isFailure)

        val message = result.exceptionOrNull()?.message.orEmpty()
        // Error message must not contain the sensitive key or URL parameters
        assertFalse("Error message leaked the API key!", message.contains(dummyKey))
        assertFalse("Error message leaked the URL key parameter!", message.contains("key="))
        assertTrue("Expected sanitized generic message, got: $message", message.contains("API request error"))
    }

    @Test
    fun sendMessageStream_whenApiKeyMissing_throwsException() = runTest {
        val emptyKeyManager = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
        )
        val repository = GeminiRepositoryImpl(apiKeyManager = emptyKeyManager)

        try {
            repository.sendMessageStream("Hello").collect {}
            org.junit.Assert.fail("Expected exception when API key is missing")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("not configured", ignoreCase = true) == true)
        }
    }

    @Test
    fun sendMessageStream_whenKeyContainsSensitiveInfo_sanitizesErrorMessage() = runTest {
        val dummyKey = "AIzaSyTestValidLengthKey123456789"
        val manager = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
        ).apply {
            storeApiKey(dummyKey)
        }

        val repository = GeminiRepositoryImpl(
            apiKeyManager = manager,
            modelFactory = { _, _ ->
                throw RuntimeException("HTTP 400 at https://generativelanguage.googleapis.com/v1beta/models?key=$dummyKey")
            },
        )

        try {
            repository.sendMessageStream("Test prompt").collect {}
            org.junit.Assert.fail("Expected exception from failing modelFactory")
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            assertFalse("Stream error leaked the API key!", message.contains(dummyKey))
            assertFalse("Stream error leaked the URL key parameter!", message.contains("key="))
            assertTrue("Expected sanitized generic message, got: $message", message.contains("API request error"))
        }
    }

    @Test
    fun chatSessionLifecycle_reusesSessionAcrossCalls_andRecreatesAfterReset() = runTest {
        val dummyKey = "AIzaSyTestValidLengthKey123456789"
        val manager = SecureApiKeyManager(
            keyProvider = InMemoryKeyProvider(),
        ).apply {
            storeApiKey(dummyKey)
        }

        var sessionCreationCount = 0
        val repository = GeminiRepositoryImpl(
            apiKeyManager = manager,
            modelFactory = { modelName, apiKey -> com.google.ai.client.generativeai.GenerativeModel(modelName, apiKey) },
            chatSessionFactory = { activeModel ->
                sessionCreationCount++
                activeModel.startChat()
            },
        )

        // Turn 1: Starts first session
        try {
            repository.sendMessageStream("Turn 1").collect {}
        } catch (_: Exception) {
            // Expected offline/network failure during stream
        }
        org.junit.Assert.assertEquals(1, sessionCreationCount)

        // Turn 2: Reuses the active session (sessionCreationCount remains 1)
        try {
            repository.sendMessageStream("Turn 2").collect {}
        } catch (_: Exception) {
            // Expected offline/network failure during stream
        }
        org.junit.Assert.assertEquals(1, sessionCreationCount)

        // Reset session
        repository.resetChatSession()

        // Turn 3: Starts a fresh session (sessionCreationCount increments to 2)
        try {
            repository.sendMessageStream("Turn 3").collect {}
        } catch (_: Exception) {
            // Expected offline/network failure during stream
        }
        org.junit.Assert.assertEquals(2, sessionCreationCount)
    }
}
