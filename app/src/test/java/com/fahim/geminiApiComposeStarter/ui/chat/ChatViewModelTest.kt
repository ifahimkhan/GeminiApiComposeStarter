package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGeminiRepository(
        var resultToReturn: Result<String> = Result.success("Fake response"),
    ) : GeminiRepository {
        var callCount = 0
        var lastPromptReceived: String? = null
        var lastHistoryReceived: List<ChatMessage> = emptyList()

        override suspend fun generateText(prompt: String): Result<String> {
            return generateText(prompt, emptyList())
        }

        override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
            callCount++
            lastPromptReceived = prompt
            lastHistoryReceived = history
            return resultToReturn
        }
    }

    @Test
    fun initialState_isEmpty() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.chatError)
    }

    @Test
    fun onPromptChange_updatesPrompt() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsEmptyError() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun onSend_success_updatesMessagesAndClearsPrompt() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("Generated text"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.chatError)
        assertEquals(1, repo.callCount)
        assertEquals("Tell me a joke", repo.lastPromptReceived)

        // Verify conversational messages
        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Tell me a joke", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertTrue(messages[0].id.isNotBlank())
        assertEquals("Generated text", messages[1].text)
        assertEquals(Participant.ASSISTANT, messages[1].participant)
        assertTrue(messages[1].id.isNotBlank())
    }

    @Test
    fun onSend_networkFailure_setsRetryableError() = runTest(testDispatcher) {
        val networkError = java.io.IOException("Network failure")
        val repo = FakeGeminiRepository(Result.failure(networkError))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        val error = viewModel.uiState.value.chatError
        assertTrue("Network error should be Retryable", error is ChatError.Retryable)
        assertTrue(error!!.message.contains("Network failure"))
    }

    @Test
    fun onSend_authFailure_setsNonRetryableError() = runTest(testDispatcher) {
        val authError = IllegalArgumentException("Invalid API key credentials. Please verify your GEMINI_API_KEY.")
        val repo = FakeGeminiRepository(Result.failure(authError))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        val error = viewModel.uiState.value.chatError
        assertTrue("Auth error should be NonRetryable", error is ChatError.NonRetryable)
    }

    @Test
    fun retry_reSendsLastPrompt() = runTest(testDispatcher) {
        val networkError = java.io.IOException("First error")
        val repo = FakeGeminiRepository(Result.failure(networkError))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("First question")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(1, repo.callCount)
        assertTrue(viewModel.uiState.value.chatError is ChatError.Retryable)

        // Now repository will succeed on retry
        repo.resultToReturn = Result.success("Success on retry")
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repo.callCount)
        assertEquals("First question", repo.lastPromptReceived)
        assertNull(viewModel.uiState.value.chatError)
    }

    // §2.3 — Voice errors must be NonRetryable and must not offer a Retry path
    @Test
    fun onVoiceError_setsNonRetryableError_retryIgnoresIt() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("answer"))
        val viewModel = ChatViewModel(repo)

        // Send one successful message first so lastPrompt is set
        viewModel.onPromptChange("Test question")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(1, repo.callCount)

        // Simulate a voice error
        viewModel.onVoiceError("Speech recognition isn't available on this device.")

        val error = viewModel.uiState.value.chatError
        assertTrue("Voice error must be NonRetryable", error is ChatError.NonRetryable)
        assertEquals("Speech recognition isn't available on this device.", error!!.message)

        // Calling retry() with a NonRetryable error must NOT trigger another API call
        viewModel.retry()
        advanceUntilIdle()

        assertEquals("retry() must not re-send when error is NonRetryable", 1, repo.callCount)
    }

    @Test
    fun onVoiceResult_whenPromptEmpty_setsPromptAndDoesNotSend() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onVoiceResult("Hello from speech recognition")

        assertEquals("Hello from speech recognition", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
        assertEquals(0, repo.callCount)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onVoiceResult_whenPromptNotEmpty_appendsToExistingPrompt() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Write a poem about")
        viewModel.onVoiceResult("the night sky")

        assertEquals("Write a poem about the night sky", viewModel.uiState.value.prompt)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun clearError_resetsErrorToNull() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onVoiceError("Temporary error")
        assertNotNull(viewModel.uiState.value.chatError)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.chatError)
    }

    // §2.1 — isRetryableError classification
    @Test
    fun isRetryableError_ioException_isTrue() {
        assertTrue(ChatViewModel.isRetryableError(java.io.IOException("timeout")))
    }

    @Test
    fun isRetryableError_quotaIllegalState_isTrue() {
        assertTrue(ChatViewModel.isRetryableError(IllegalStateException("Rate limit or quota exceeded.")))
    }

    @Test
    fun isRetryableError_illegalArgument_isFalse() {
        assertFalse(ChatViewModel.isRetryableError(IllegalArgumentException("Invalid API key")))
    }

    @Test
    fun isRetryableError_securityException_isFalse() {
        assertFalse(ChatViewModel.isRetryableError(SecurityException("Permission denied")))
    }

    private class FakeChatHistoryRepository(
        initialMessages: List<ChatMessage> = emptyList(),
    ) : ChatHistoryRepository {
        private val messagesFlow = MutableStateFlow(initialMessages)

        override fun getChatHistory(): Flow<List<ChatMessage>> =
            messagesFlow.asStateFlow()

        override suspend fun insertMessage(message: ChatMessage) {
            messagesFlow.update { it + message }
        }

        override suspend fun clearHistory() {
            messagesFlow.update { emptyList() }
        }
    }

    @Test
    fun viewModelStartup_restoresPersistedConversationHistory() = runTest(testDispatcher) {
        val persistedMessages = listOf(
            ChatMessage(id = "1", text = "Previous user message", participant = Participant.USER),
            ChatMessage(id = "2", text = "Previous assistant reply", participant = Participant.ASSISTANT),
        )
        val historyRepo = FakeChatHistoryRepository(persistedMessages)
        val geminiRepo = FakeGeminiRepository()

        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Previous user message", viewModel.uiState.value.messages[0].text)
        assertEquals("Previous assistant reply", viewModel.uiState.value.messages[1].text)
    }

    @Test
    fun onSend_successfulSend_persistsBothTurns() = runTest(testDispatcher) {
        val historyRepo = FakeChatHistoryRepository()
        val geminiRepo = FakeGeminiRepository(Result.success("AI response"))
        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("User query")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("User query", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("AI response", messages[1].text)
        assertEquals(Participant.ASSISTANT, messages[1].participant)
    }

    @Test
    fun onSend_failedRequest_doesNotPersistFakeGeminiResponse() = runTest(testDispatcher) {
        val historyRepo = FakeChatHistoryRepository()
        val geminiRepo = FakeGeminiRepository(Result.failure(java.io.IOException("Network error")))
        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Failed query")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(1, messages.size)
        assertEquals("Failed query", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertNotNull(viewModel.uiState.value.chatError)
    }

    @Test
    fun clearChat_clearsDatabaseAndUiState() = runTest(testDispatcher) {
        val initialMessages = listOf(
            ChatMessage(id = "1", text = "Msg 1", participant = Participant.USER),
            ChatMessage(id = "2", text = "Msg 2", participant = Participant.ASSISTANT),
        )
        val historyRepo = FakeChatHistoryRepository(initialMessages)
        val geminiRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearChat()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun retry_withPersistence_persistsAssistantResponseCorrectly() = runTest(testDispatcher) {
        val historyRepo = FakeChatHistoryRepository()
        val geminiRepo = FakeGeminiRepository(Result.failure(java.io.IOException("First failure")))
        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Question to retry")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertTrue(viewModel.uiState.value.chatError is ChatError.Retryable)

        // On retry, Gemini succeeds
        geminiRepo.resultToReturn = Result.success("Success on second attempt")
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Question to retry", viewModel.uiState.value.messages[0].text)
        assertEquals("Success on second attempt", viewModel.uiState.value.messages[1].text)
    }

    @Test
    fun onSend_whenAlreadyLoading_ignoresSubsequentSend() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("First answer"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("First question")
        viewModel.onSend()
        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt second send while first is still loading
        viewModel.onPromptChange("Second question")
        viewModel.onSend()

        advanceUntilIdle()

        // Call count should be exactly 1, ignoring concurrent send
        assertEquals(1, repo.callCount)
        assertEquals("First question", repo.lastPromptReceived)
    }

    @Test
    fun messageIds_areUniqueAcrossTurns() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("Answer"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Turn 1")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onPromptChange("Turn 2")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(4, messages.size)
        val uniqueIds = messages.map { it.id }.toSet()
        assertEquals(4, uniqueIds.size)
    }

    @Test
    fun onSend_secondTurn_passesPriorConversationHistoryToRepository() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("4"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("What is 2 + 2?")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("What is 2 + 2?", repo.lastPromptReceived)
        assertTrue(repo.lastHistoryReceived.isEmpty())

        // Send second turn
        repo.resultToReturn = Result.success("40")
        viewModel.onPromptChange("Multiply that by 10")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals("Multiply that by 10", repo.lastPromptReceived)
        assertEquals(2, repo.lastHistoryReceived.size)
        assertEquals("What is 2 + 2?", repo.lastHistoryReceived[0].text)
        assertEquals(Participant.USER, repo.lastHistoryReceived[0].participant)
        assertEquals("4", repo.lastHistoryReceived[1].text)
        assertEquals(Participant.ASSISTANT, repo.lastHistoryReceived[1].participant)
    }

    @Test
    fun retry_passesPriorConversationHistoryToRepository() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("4"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("What is 2 + 2?")
        viewModel.onSend()
        advanceUntilIdle()

        // Turn 2 fails with retryable error
        repo.resultToReturn = Result.failure(java.io.IOException("Network error"))
        viewModel.onPromptChange("Multiply that by 10")
        viewModel.onSend()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.chatError is ChatError.Retryable)

        // Retry turn 2
        repo.resultToReturn = Result.success("40")
        viewModel.retry()
        advanceUntilIdle()

        assertEquals("Multiply that by 10", repo.lastPromptReceived)
        assertEquals(2, repo.lastHistoryReceived.size)
        assertEquals("What is 2 + 2?", repo.lastHistoryReceived[0].text)
        assertEquals(Participant.USER, repo.lastHistoryReceived[0].participant)
        assertEquals("4", repo.lastHistoryReceived[1].text)
        assertEquals(Participant.ASSISTANT, repo.lastHistoryReceived[1].participant)
    }

    @Test
    fun isRetryableError_withTypedGeminiExceptions_classifiesCorrectly() {
        // Retryable: Network, Overloaded, QuotaExceeded
        assertTrue(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.Network()))
        assertTrue(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.Overloaded()))
        assertTrue(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.QuotaExceeded()))

        // Non-retryable: Authentication, PermissionDenied, ModelNotFound, BadRequest, ContentBlocked, Unknown
        assertFalse(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.Authentication()))
        assertFalse(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.PermissionDenied()))
        assertFalse(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.ModelNotFound()))
        assertFalse(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.BadRequest()))
        assertFalse(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.ContentBlocked()))
        assertFalse(ChatViewModel.isRetryableError(com.fahim.geminiApiComposeStarter.data.GeminiException.Unknown("Unclassified")))
    }

    @Test
    fun clearChat_whileRequestInFlight_cancelsJobAndPreventsStaleMessages() = runTest(testDispatcher) {
        val suspendingRepo = object : GeminiRepository {
            override suspend fun generateText(prompt: String): Result<String> = generateText(prompt, emptyList())
            override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
                kotlinx.coroutines.delay(5000L)
                return Result.success("Late assistant response")
            }
        }
        val viewModel = ChatViewModel(suspendingRepo)

        viewModel.onPromptChange("In-flight prompt")
        viewModel.onSend()
        testScheduler.advanceTimeBy(1000L)
        assertTrue("ViewModel should be loading while request is in-flight", viewModel.uiState.value.isLoading)

        // User clears conversation while request is still pending
        viewModel.clearChat()
        advanceUntilIdle()

        val stateAfterCompletion = viewModel.uiState.value
        assertEquals("Chat history must remain empty after in-flight request was cancelled", 0, stateAfterCompletion.messages.size)
        assertFalse("Loading must be reset after clearChat", stateAfterCompletion.isLoading)
        assertNull("No error should be recorded from cancelled request", stateAfterCompletion.chatError)
    }

    @Test
    fun onSend_whileRequestInFlight_ignoresConcurrentSend() = runTest(testDispatcher) {
        var calls = 0
        val suspendingRepo = object : GeminiRepository {
            override suspend fun generateText(prompt: String): Result<String> = generateText(prompt, emptyList())
            override suspend fun generateText(prompt: String, history: List<ChatMessage>): Result<String> {
                calls++
                kotlinx.coroutines.delay(2000L)
                return Result.success("Response $calls")
            }
        }
        val viewModel = ChatViewModel(suspendingRepo)

        viewModel.onPromptChange("First prompt")
        viewModel.onSend()
        testScheduler.advanceTimeBy(500L)
        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt second rapid send
        viewModel.onPromptChange("Second prompt")
        viewModel.onSend()

        advanceUntilIdle()

        assertEquals("Only the initial prompt should have been dispatched", 1, calls)
        assertEquals(2, viewModel.uiState.value.messages.size) // 1 user + 1 assistant
        assertEquals("First prompt", viewModel.uiState.value.messages[0].text)
        assertEquals("Response 1", viewModel.uiState.value.messages[1].text)
    }
}
