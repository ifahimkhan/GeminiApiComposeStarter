package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.security.ApiKeyProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRepositoryImplTest {

    @Test
    fun generateText_missingApiKey_returnsFailureWithActionableMessage() = runTest {
        val missingKeyProvider = object : ApiKeyProvider {
            override fun getApiKey(): String? = null
            override fun hasApiKey(): Boolean = false
        }

        val repository = GeminiRepositoryImpl(apiKeyProvider = missingKeyProvider)
        val result = repository.generateText("Hello")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message
        assertEquals("Gemini API key is not configured. Please add GEMINI_API_KEY to local.properties.", message)
    }

    @Test
    fun generateText_blankApiKey_returnsFailureWithActionableMessage() = runTest {
        val blankKeyProvider = object : ApiKeyProvider {
            override fun getApiKey(): String = "   "
            override fun hasApiKey(): Boolean = false
        }

        val repository = GeminiRepositoryImpl(apiKeyProvider = blankKeyProvider)
        val result = repository.generateText("Hello")

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message
        assertEquals("Gemini API key is not configured. Please add GEMINI_API_KEY to local.properties.", message)
    }
}
