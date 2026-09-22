package com.fahim.geminiApiComposeStarter.data

import com.fahim.geminiApiComposeStarter.BuildConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Talks to the real Gemini API, so it is skipped unless you opt in:
 *   RUN_LIVE_TESTS=1 ./gradlew :app:testDebugUnitTest --tests "*GeminiLiveFallbackTest*"
 */
class GeminiLiveFallbackTest {

    @Test
    fun unknownFirstModel_fallsBackToARealOne() = runBlocking {
        assumeTrue(System.getenv("RUN_LIVE_TESTS") == "1")
        assumeTrue(BuildConfig.GEMINI_API_KEY.isNotBlank())

        val repository = GeminiRepositoryImpl(
            apiKey = BuildConfig.GEMINI_API_KEY,
            modelNames = listOf("gemini-this-model-does-not-exist", "gemini-3.1-flash-lite"),
        )
        val result = repository.generateText("Reply with the single word: pong")

        println("live fallback result ok=${result.isSuccess}")
        assertTrue("expected a real answer after falling back", result.isSuccess)
    }
}
