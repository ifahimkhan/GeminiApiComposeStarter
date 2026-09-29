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

        override suspend fun generateText(prompt: String): Result<String> {
            callCount++
            lastPromptReceived = prompt
            return resultToReturn
        }
    }

    @Test
    fun initialState_isEmpty() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals("", state.response)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
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
    fun onSend_success_updatesResponseAndClearsPrompt() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.success("Generated text"))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Generated text", viewModel.uiState.value.response)
        assertNull(viewModel.uiState.value.errorMessage)
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
    fun onSend_failure_updatesErrorMessage() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.failure(RuntimeException("Network failure")))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network failure", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun retry_reSendsLastPrompt() = runTest(testDispatcher) {
        val repo = FakeGeminiRepository(Result.failure(RuntimeException("First error")))
        val viewModel = ChatViewModel(repo)

        viewModel.onPromptChange("First question")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(1, repo.callCount)
        assertEquals("First error", viewModel.uiState.value.errorMessage)

        // Now repository will succeed on retry
        repo.resultToReturn = Result.success("Success on retry")
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repo.callCount)
        assertEquals("First question", repo.lastPromptReceived)
        assertEquals("Success on retry", viewModel.uiState.value.response)
        assertNull(viewModel.uiState.value.errorMessage)
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
    fun onVoiceError_setsErrorMessage() {
        val repo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repo)

        viewModel.onVoiceError("Speech recognition isn't available on this device.")

        assertEquals(
            "Speech recognition isn't available on this device.",
            viewModel.uiState.value.errorMessage
        )
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
        val geminiRepo = FakeGeminiRepository(Result.failure(RuntimeException("Network error")))
        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Failed query")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(1, messages.size)
        assertEquals("Failed query", messages[0].text)
        assertEquals(Participant.USER, messages[0].participant)
        assertEquals("Network error", viewModel.uiState.value.errorMessage)
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
        val geminiRepo = FakeGeminiRepository(Result.failure(RuntimeException("First failure")))
        val viewModel = ChatViewModel(geminiRepo, historyRepo)
        advanceUntilIdle()

        viewModel.onPromptChange("Question to retry")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)

        // On retry, Gemini succeeds
        geminiRepo.resultToReturn = Result.success("Success on second attempt")
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Question to retry", viewModel.uiState.value.messages[0].text)
        assertEquals("Success on second attempt", viewModel.uiState.value.messages[1].text)
    }
}

