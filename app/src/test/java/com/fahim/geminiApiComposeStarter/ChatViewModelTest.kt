package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.room.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.room.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.room.MessageRole
import com.fahim.geminiApiComposeStarter.data.repository.ChatRepositoryImpl
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

/**
 * Unit tests for ChatViewModel using kotlinx-coroutines-test and FakeGeminiRepository.
 * Covers all required assignment states: initial, loading, success, failure, retry,
 * empty input, missing API key, duplicate send protection, and voice input.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeChatDao : ChatDao {
        val messagesFlowState = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
        override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlowState

        override suspend fun insertMessage(message: ChatMessageEntity): Long {
            val id = (messagesFlowState.value.maxOfOrNull { it.id } ?: 0L) + 1L
            val copy = message.copy(id = id)
            messagesFlowState.update { it + copy }
            return id
        }

        override suspend fun deleteMessageById(id: Long): Int {
            val before = messagesFlowState.value.size
            messagesFlowState.update { list -> list.filterNot { it.id == id } }
            return before - messagesFlowState.value.size
        }

        override suspend fun clearAll(): Int {
            val count = messagesFlowState.value.size
            messagesFlowState.value = emptyList()
            return count
        }

        override suspend fun getLastMessage(): ChatMessageEntity? =
            messagesFlowState.value.lastOrNull()
    }

    private class FakeGeminiRepository : GeminiRepository {
        var shouldFail: Boolean = false
        var failureMessage: String = "Network error occurred"
        var delayMillis: Long = 0
        var cannedResponse: String = "Test response from Gemini"
        val promptsReceived = mutableListOf<String>()

        override suspend fun generateText(prompt: String): Result<String> {
            promptsReceived.add(prompt)
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            return if (shouldFail) {
                Result.failure(Exception(failureMessage))
            } else {
                Result.success(cannedResponse)
            }
        }
    }

    private lateinit var fakeDao: FakeChatDao
    private lateinit var fakeGeminiRepo: FakeGeminiRepository
    private lateinit var chatRepository: ChatRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeChatDao()
        fakeGeminiRepo = FakeGeminiRepository()
        chatRepository = ChatRepositoryImpl(fakeGeminiRepo, fakeDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasExpectedDefaults() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertNull(state.promptError)
        assertNull(state.errorMessage)
        assertNull(state.lastFailedPrompt)
        assertTrue(state.hasApiKey)
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsEmptyPromptError() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("   ")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeGeminiRepo.promptsReceived.isEmpty())
    }

    @Test
    fun onSend_missingApiKey_showsMissingKeyError() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository, hasApiKey = false)

        viewModel.onPromptChange("Hello without API key")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(fakeGeminiRepo.promptsReceived.isEmpty())
    }

    @Test
    fun onSend_successfulResponse_persistsMessagesAndResetsLoading() = runTest(testDispatcher) {
        fakeGeminiRepo.cannedResponse = "Gemini is ready to help!"
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("What is Android?")
        viewModel.onSend()

        // Prompt input cleared immediately
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.messages.size)

        val userMessage = state.messages[0]
        assertEquals("What is Android?", userMessage.content)
        assertEquals(MessageRole.USER, userMessage.role)

        val geminiMessage = state.messages[1]
        assertEquals("Gemini is ready to help!", geminiMessage.content)
        assertEquals(MessageRole.GEMINI, geminiMessage.role)
        assertFalse(geminiMessage.isError)
    }

    @Test
    fun onSend_failedResponse_setsErrorAndAllowsRetry() = runTest(testDispatcher) {
        fakeGeminiRepo.shouldFail = true
        fakeGeminiRepo.failureMessage = "Quota exceeded (429)"
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("Trigger error")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Quota exceeded (429)", state.errorMessage)
        assertEquals("Trigger error", state.lastFailedPrompt)

        // Verify user prompt and error response were stored
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[1].isError)
    }

    @Test
    fun onSend_loadingState_preventsDuplicateSendRequests() = runTest(testDispatcher) {
        fakeGeminiRepo.delayMillis = 500
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("Initial prompt")
        viewModel.onSend()

        // First send made it loading
        assertTrue(viewModel.uiState.value.isLoading)

        // Attempt second send while first is in flight
        viewModel.onPromptChange("Second duplicate prompt")
        viewModel.onSend()

        advanceUntilIdle()

        // Only the first prompt should have been processed by the repository
        assertEquals(1, fakeGeminiRepo.promptsReceived.size)
        assertEquals("Initial prompt", fakeGeminiRepo.promptsReceived[0])
    }

    @Test
    fun onRetry_retriesFailedPrompt() = runTest(testDispatcher) {
        fakeGeminiRepo.shouldFail = true
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("Prompt that will fail")
        viewModel.onSend()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.lastFailedPrompt)

        // Now fix repo failure and retry
        fakeGeminiRepo.shouldFail = false
        fakeGeminiRepo.cannedResponse = "Recovered successfully!"
        viewModel.onRetry()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.lastFailedPrompt)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, fakeGeminiRepo.promptsReceived.size)
    }

    @Test
    fun onVoiceResult_insertsRecognizedTextIntoPrompt() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onVoiceResult("Spoken query from voice")
        assertEquals("Spoken query from voice", viewModel.uiState.value.prompt)

        // Appending subsequent speech
        viewModel.onVoiceResult("part two")
        assertEquals("Spoken query from voice part two", viewModel.uiState.value.prompt)
    }

    @Test
    fun onClearHistory_removesAllMessages() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun onDismissError_clearsErrorMessage() = runTest(testDispatcher) {
        fakeGeminiRepo.shouldFail = true
        val viewModel = ChatViewModel(chatRepository, hasApiKey = true)

        viewModel.onPromptChange("Will fail")
        viewModel.onSend()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.onDismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
