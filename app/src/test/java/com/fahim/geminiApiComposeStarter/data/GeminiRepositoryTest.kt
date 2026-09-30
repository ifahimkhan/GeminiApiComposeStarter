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
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.reflect.KClass
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.google.ai.client.generativeai.type.BlockReason
import com.google.ai.client.generativeai.type.Candidate
import com.google.ai.client.generativeai.type.CitationMetadata
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.FinishReason
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.InvalidAPIKeyException
import com.google.ai.client.generativeai.type.PromptBlockedException
import com.google.ai.client.generativeai.type.PromptFeedback
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.SafetyRating
import com.google.ai.client.generativeai.type.SerializationException
import com.google.ai.client.generativeai.type.ServerException
import com.google.ai.client.generativeai.type.UnknownException
import com.google.ai.client.generativeai.type.UnsupportedUserLocationException

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

    private class FakeContentGenerator(
        private val responses: MutableMap<String, MutableList<() -> String>> = mutableMapOf(),
    ) : ContentGenerator {
        val callLog = mutableListOf<String>()

        fun addResponse(model: String, action: () -> String) {
            responses.getOrPut(model) { mutableListOf() }.add(action)
        }

        override suspend fun generateContent(
            modelName: String,
            apiKey: String,
            prompt: String,
            history: List<com.google.ai.client.generativeai.type.Content>,
        ): String {
            callLog.add(modelName)
            val queue = responses[modelName]
            if (queue != null && queue.isNotEmpty()) {
                val action = queue.removeAt(0)
                return action()
            }
            return "Default answer from $modelName"
        }
    }

    @Test
    fun generateText_transientOverloadedFailure_retriesAndSucceedsOnAttempt2() = runTest(testDispatcher) {
        val fakeGenerator = FakeContentGenerator().apply {
            addResponse(GeminiConfig.MODEL_NAME) { throw GeminiException.Overloaded("503 UNAVAILABLE") }
            addResponse(GeminiConfig.MODEL_NAME) { "Recovered response" }
        }
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider("test-key"),
            ioDispatcher = testDispatcher,
            contentGenerator = fakeGenerator,
        )

        val result = repo.generateText("Hello")
        assertTrue("Transient 503 must succeed on attempt 2", result.isSuccess)
        assertEquals("Recovered response", result.getOrNull())
        assertEquals(2, fakeGenerator.callLog.size)
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[0])
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[1])
    }

    @Test
    fun generateText_persistent503_switchesToFallbackModel() = runTest(testDispatcher) {
        val fakeGenerator = FakeContentGenerator().apply {
            addResponse(GeminiConfig.MODEL_NAME) { throw GeminiException.Overloaded("503 UNAVAILABLE") }
            addResponse(GeminiConfig.MODEL_NAME) { throw GeminiException.Overloaded("503 UNAVAILABLE") }
            addResponse(GeminiConfig.FALLBACK_MODEL_NAME) { "Fallback model success" }
        }
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider("test-key"),
            ioDispatcher = testDispatcher,
            contentGenerator = fakeGenerator,
        )

        val result = repo.generateText("Hello")
        assertTrue("Fallback model must succeed", result.isSuccess)
        assertEquals("Fallback model success", result.getOrNull())
        assertEquals(3, fakeGenerator.callLog.size)
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[0])
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[1])
        assertEquals(GeminiConfig.FALLBACK_MODEL_NAME, fakeGenerator.callLog[2])
    }

    @Test
    fun generateText_quotaExceededOnPrimary_switchesToFallbackModel() = runTest(testDispatcher) {
        val fakeGenerator = FakeContentGenerator().apply {
            addResponse(GeminiConfig.MODEL_NAME) { throw GeminiException.QuotaExceeded("429 RESOURCE_EXHAUSTED") }
            addResponse(GeminiConfig.FALLBACK_MODEL_NAME) { "Fallback success after quota" }
        }
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider("test-key"),
            ioDispatcher = testDispatcher,
            contentGenerator = fakeGenerator,
        )

        val result = repo.generateText("Hello")
        assertTrue("Fallback model must succeed on quota exhausted", result.isSuccess)
        assertEquals("Fallback success after quota", result.getOrNull())
        assertEquals(2, fakeGenerator.callLog.size)
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[0])
        assertEquals(GeminiConfig.FALLBACK_MODEL_NAME, fakeGenerator.callLog[1])
    }

    @Test
    fun generateText_modelNotFoundOnPrimary_switchesToFallbackModel() = runTest(testDispatcher) {
        val fakeGenerator = FakeContentGenerator().apply {
            addResponse(GeminiConfig.MODEL_NAME) { throw GeminiException.ModelNotFound("404 NOT_FOUND") }
            addResponse(GeminiConfig.FALLBACK_MODEL_NAME) { "Fallback success after 404" }
        }
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider("test-key"),
            ioDispatcher = testDispatcher,
            contentGenerator = fakeGenerator,
        )

        val result = repo.generateText("Hello")
        assertTrue("Fallback model must succeed on 404", result.isSuccess)
        assertEquals("Fallback success after 404", result.getOrNull())
        assertEquals(2, fakeGenerator.callLog.size)
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[0])
        assertEquals(GeminiConfig.FALLBACK_MODEL_NAME, fakeGenerator.callLog[1])
    }

    @Test
    fun generateText_nonRetryableAuthError_failsImmediatelyWithoutRetryOrFallback() = runTest(testDispatcher) {
        val fakeGenerator = FakeContentGenerator().apply {
            addResponse(GeminiConfig.MODEL_NAME) { throw GeminiException.Authentication("401 API_KEY_INVALID") }
            addResponse(GeminiConfig.FALLBACK_MODEL_NAME) { "Should not be called" }
        }
        val repo = GeminiRepositoryImpl(
            apiKeyProvider = FakeApiKeyProvider("test-key"),
            ioDispatcher = testDispatcher,
            contentGenerator = fakeGenerator,
        )

        val result = repo.generateText("Hello")
        assertTrue("Auth error must return failure", result.isFailure)
        assertTrue(result.exceptionOrNull() is GeminiException.Authentication)
        assertEquals("Auth failure must abort after 1 attempt with NO fallback", 1, fakeGenerator.callLog.size)
        assertEquals(GeminiConfig.MODEL_NAME, fakeGenerator.callLog[0])
    }

    @Test
    fun errorClassification_retryabilityMatrix_strictlyAdheresToSpecification() {
        // Retryable errors
        assertTrue(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.Network()))
        assertTrue(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.Overloaded()))
        assertTrue(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.QuotaExceeded()))

        // Non-retryable errors
        org.junit.Assert.assertFalse(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.Authentication()))
        org.junit.Assert.assertFalse(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.PermissionDenied()))
        org.junit.Assert.assertFalse(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.ModelNotFound()))
        org.junit.Assert.assertFalse(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.ContentBlocked()))
        org.junit.Assert.assertFalse(com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel.isRetryableError(GeminiException.Unknown("Unclassified")))
    }

    private data class SdkExceptionTestCase(
        val name: String,
        val rawException: Exception,
        val expectedType: KClass<out GeminiException>,
        val expectedRetryable: Boolean,
    )

    /**
     * Phase 6: Real SDK Exception Mapping Verification.
     * Table-driven suite constructing REAL SDK exceptions and wrapped root causes,
     * asserting both the mapped GeminiException domain type and retryability classification.
     */
    @Test
    fun realSdkExceptionMapping_tableDriven_mapsToCorrectGeminiExceptionAndRetryability() {
        val repo = GeminiRepositoryImpl(apiKeyProvider = FakeApiKeyProvider("test-key"))

        val safetyCandidate = try {
            val ctor = Candidate::class.java.declaredConstructors[0].apply { isAccessible = true }
            ctor.newInstance(
                Content(role = "model", parts = emptyList()),
                emptyList<SafetyRating>(),
                emptyList<CitationMetadata>(),
                FinishReason.SAFETY,
            ) as Candidate
        } catch (_: Throwable) {
            null
        }

        val testCases = mutableListOf(
            SdkExceptionTestCase(
                name = "InvalidAPIKeyException",
                rawException = InvalidAPIKeyException("API Key not valid"),
                expectedType = GeminiException.Authentication::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "ServerException 503 Service Unavailable",
                rawException = ServerException("503 Service Unavailable"),
                expectedType = GeminiException.Overloaded::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "ServerException 404 Model Not Found",
                rawException = ServerException("404 Model Not Found"),
                expectedType = GeminiException.ModelNotFound::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "ServerException 403 Forbidden",
                rawException = ServerException("403 Forbidden"),
                expectedType = GeminiException.PermissionDenied::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "QuotaExceededException",
                rawException = QuotaExceededException("Quota exceeded for quota metric"),
                expectedType = GeminiException.QuotaExceeded::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "UnsupportedUserLocationException",
                rawException = UnsupportedUserLocationException(),
                expectedType = GeminiException.PermissionDenied::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "PromptBlockedException",
                rawException = PromptBlockedException(
                    GenerateContentResponse(
                        candidates = emptyList(),
                        promptFeedback = PromptFeedback(BlockReason.SAFETY, emptyList()),
                        usageMetadata = null,
                    )
                ),
                expectedType = GeminiException.ContentBlocked::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "SerializationException (MissingFieldException)",
                rawException = SerializationException("MissingFieldException: Field 'candidates' is required"),
                expectedType = GeminiException.Overloaded::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "Wrapped UnknownHostException inside UnknownException",
                rawException = UnknownException("Something unexpected happened.", UnknownHostException("Unable to resolve host")),
                expectedType = GeminiException.Network::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "Wrapped SocketTimeoutException inside UnknownException",
                rawException = UnknownException("Something unexpected happened.", SocketTimeoutException("Read timed out")),
                expectedType = GeminiException.Network::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "Wrapped ConnectException inside UnknownException",
                rawException = UnknownException("Something unexpected happened.", ConnectException("Connection refused")),
                expectedType = GeminiException.Network::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "Wrapped generic IOException inside UnknownException",
                rawException = UnknownException("Something unexpected happened.", IOException("Broken pipe")),
                expectedType = GeminiException.Network::class,
                expectedRetryable = true,
            ),
            SdkExceptionTestCase(
                name = "ServerException with 400 Bad Request maps to BadRequest and is NonRetryable",
                rawException = ServerException("400 Bad Request: Invalid argument supplied"),
                expectedType = GeminiException.BadRequest::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "Exception containing INVALID_ARGUMENT maps to BadRequest and is NonRetryable",
                rawException = IllegalArgumentException("INVALID_ARGUMENT: Value out of range"),
                expectedType = GeminiException.BadRequest::class,
                expectedRetryable = false,
            ),
            SdkExceptionTestCase(
                name = "Unclassified UnknownException defaults to Unknown and is NonRetryable",
                rawException = UnknownException("Unclassified error message", null),
                expectedType = GeminiException.Unknown::class,
                expectedRetryable = false,
            ),
        )

        if (safetyCandidate != null) {
            testCases.add(
                SdkExceptionTestCase(
                    name = "ResponseStoppedException due to safety finish reason",
                    rawException = ResponseStoppedException(
                        GenerateContentResponse(
                            candidates = listOf(safetyCandidate),
                            promptFeedback = null,
                            usageMetadata = null,
                        )
                    ),
                    expectedType = GeminiException.ContentBlocked::class,
                    expectedRetryable = false,
                )
            )
        }

        for (tc in testCases) {
            val mapped = repo.mapExceptionToUserFriendlyError(tc.rawException)
            assertTrue(
                "Test case '${tc.name}' expected ${tc.expectedType.simpleName} but got ${mapped::class.simpleName}",
                tc.expectedType.isInstance(mapped),
            )
            val retryable = ChatViewModel.isRetryableError(mapped)
            assertEquals(
                "Test case '${tc.name}' expected retryable=${tc.expectedRetryable} but got $retryable",
                tc.expectedRetryable,
                retryable,
            )
        }
    }
}
