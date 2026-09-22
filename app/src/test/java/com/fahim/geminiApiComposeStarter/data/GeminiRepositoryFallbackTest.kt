package com.fahim.geminiApiComposeStarter.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRepositoryFallbackTest {

    private val models = listOf("primary", "second", "third")

    /** Builds a repository whose models behave as scripted; records which models were asked. */
    private fun repo(calls: MutableList<String>, behaviour: (String) -> String?): GeminiRepositoryImpl =
        GeminiRepositoryImpl(models) { model, _ ->
            calls += model
            behaviour(model)
        }

    private fun busy() = IllegalStateException("503 This model is currently experiencing high demand")

    @Test
    fun usesPrimaryModel_whenItWorks() = runBlocking {
        val calls = mutableListOf<String>()
        val result = repo(calls) { "answer from $it" }.generateText("hi")

        assertEquals("answer from primary", result.getOrNull())
        assertEquals(listOf("primary"), calls)
    }

    @Test
    fun fallsBackToNextModel_whenPrimaryIsBusy() = runBlocking {
        val calls = mutableListOf<String>()
        val result = repo(calls) { if (it == "primary") throw busy() else "answer from $it" }.generateText("hi")

        assertEquals("answer from second", result.getOrNull())
        assertEquals(listOf("primary", "second"), calls)
    }

    @Test
    fun fallsBackThroughAllModels_untilOneWorks() = runBlocking {
        val calls = mutableListOf<String>()
        val result = repo(calls) { if (it == "third") "answer from third" else throw busy() }.generateText("hi")

        assertEquals("answer from third", result.getOrNull())
        assertEquals(models, calls)
    }

    @Test
    fun fallsBack_onTimeout() = runBlocking {
        val calls = mutableListOf<String>()
        val timeout = RuntimeException("Something unexpected", java.net.SocketTimeoutException("Socket timeout has expired"))
        val result = repo(calls) { if (it == "primary") throw timeout else "ok" }.generateText("hi")

        assertEquals("ok", result.getOrNull())
    }

    @Test
    fun fallsBack_onEmptyReply() = runBlocking {
        val calls = mutableListOf<String>()
        val result = repo(calls) { if (it == "primary") "   " else "real answer" }.generateText("hi")

        assertEquals("real answer", result.getOrNull())
    }

    @Test
    fun returnsLastError_whenEveryModelFails() = runBlocking {
        val calls = mutableListOf<String>()
        val result = repo(calls) { throw busy() }.generateText("hi")

        assertTrue(result.isFailure)
        assertEquals(models, calls)
    }

    @Test
    fun doesNotFallBack_onBadApiKey() = runBlocking {
        val calls = mutableListOf<String>()
        val result = repo(calls) { throw IllegalStateException("API key not valid. Please pass a valid API key.") }
            .generateText("hi")

        assertTrue(result.isFailure)
        assertEquals(listOf("primary"), calls)
    }
}
