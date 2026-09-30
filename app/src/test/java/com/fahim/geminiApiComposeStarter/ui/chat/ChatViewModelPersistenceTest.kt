package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.InMemoryChatHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
class ChatViewModelPersistenceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var historyRepository: InMemoryChatHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        historyRepository = InMemoryChatHistoryRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeGeminiRepository(
        private val streamChunks: List<String> = emptyList(),
        private val streamException: Exception? = null,
    ) : GeminiRepository {
        var resetCalled = false
        var lastPromptSent: String? = null

        override suspend fun generateText(prompt: String): Result<String> =
            Result.success("Static response")

        override fun sendMessageStream(prompt: String): Flow<String> = flow {
            lastPromptSent = prompt
            for (chunk in streamChunks) {
                emit(chunk)
            }
            if (streamException != null) {
                throw streamException
            }
        }

        override fun resetChatSession() {
            resetCalled = true
        }
    }

    @Test
    fun onSend_persistsUserAndCompletedAssistantMessage() = runTest {
        val fakeRepo = FakeGeminiRepository(streamChunks = listOf("Hello", " world!"))
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Hi Gemini")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("Hi Gemini", state.messages[0].text)
        assertEquals(Participant.USER, state.messages[0].participant)
        assertEquals("Hello world!", state.messages[1].text)
        assertEquals(Participant.ASSISTANT, state.messages[1].participant)
        assertFalse(state.messages[1].isPending)

        // Verify history repository has both messages
        val saved = historyRepository.getMessages()
        advanceUntilIdle()
        assertEquals(2, state.messages.size)
    }

    @Test
    fun persistenceAcrossViewModelRecreation_restoresFullHistory() = runTest {
        val fakeRepo = FakeGeminiRepository(streamChunks = listOf("I remember you!"))
        val viewModel1 = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel1.onPromptChange("Remember me?")
        viewModel1.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel1.uiState.value.messages.size)

        // Simulate ViewModel recreation (e.g. configuration change, app reload)
        val viewModel2 = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        val restoredState = viewModel2.uiState.value
        assertEquals(2, restoredState.messages.size)
        assertEquals("Remember me?", restoredState.messages[0].text)
        assertEquals(Participant.USER, restoredState.messages[0].participant)
        assertEquals("I remember you!", restoredState.messages[1].text)
        assertEquals(Participant.ASSISTANT, restoredState.messages[1].participant)
        assertFalse(restoredState.messages[1].isPending)
    }

    @Test
    fun onClearChat_clearsDatabaseSessionAndUiState() = runTest {
        val fakeRepo = FakeGeminiRepository(streamChunks = listOf("Response"))
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Test prompt")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        val clearedState = viewModel.uiState.value
        assertTrue(clearedState.messages.isEmpty())
        assertTrue(clearedState.response.isEmpty())
        assertTrue(fakeRepo.resetCalled)
    }

    @Test
    fun onSend_streamFailureWithPartialContent_persistsPartialResponse() = runTest {
        val fakeRepo = FakeGeminiRepository(
            streamChunks = listOf("Partial text before crash"),
            streamException = RuntimeException("Connection lost"),
        )
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Stream me")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Connection lost", state.errorMessage)
        assertEquals(2, state.messages.size)
        assertEquals("Partial text before crash", state.messages[1].text)
        assertFalse(state.messages[1].isPending)
    }

    @Test
    fun onSend_streamFailureWithNoContent_discardsPendingPlaceholder() = runTest {
        val fakeRepo = FakeGeminiRepository(
            streamChunks = emptyList(),
            streamException = RuntimeException("Immediate failure"),
        )
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Failing prompt")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Immediate failure", state.errorMessage)
        // User message is preserved, but empty pending assistant message was removed
        assertEquals(1, state.messages.size)
        assertEquals("Failing prompt", state.messages[0].text)
        assertEquals(Participant.USER, state.messages[0].participant)
    }

    @Test
    fun onSend_whenPromptIsEmpty_setsPromptErrorWithoutPersisting() = runTest {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("   ")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSend_whenApiKeyMissing_showsErrorWithoutPersisting() = runTest {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = false,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun duplicatePrevention_repeatedInteractionsMaintainStableKeys() = runTest {
        val fakeRepo = FakeGeminiRepository(streamChunks = listOf("Ack"))
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Turn 1")
        viewModel.onSend()
        advanceUntilIdle()

        viewModel.onPromptChange("Turn 2")
        viewModel.onSend()
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(4, messages.size)
        val distinctIds = messages.map { it.id }.distinct()
        assertEquals(4, distinctIds.size)
    }

    @Test
    fun onSend_whenAlreadyLoading_ignoresSubsequentCalls() = runTest {
        val fakeRepo = FakeGeminiRepository(streamChunks = listOf("Response 1"))
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Prompt 1")
        viewModel.onSend()

        // Immediate subsequent call while loading is true
        viewModel.onPromptChange("Prompt 2")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Prompt 1", state.messages[0].text)
        assertEquals("Response 1", state.messages[1].text)
    }

    @Test
    fun onSend_streaming_accumulatesTokensCorrectly() = runTest {
        val fakeRepo = FakeGeminiRepository(streamChunks = listOf("Chunk1", " Chunk2", " Chunk3"))
        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        advanceUntilIdle()

        viewModel.onPromptChange("Stream test")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("Chunk1 Chunk2 Chunk3", state.messages[1].text)
        assertEquals("Chunk1 Chunk2 Chunk3", state.response)
        assertFalse(state.messages[1].isPending)
    }

    @Test
    fun onClearChat_whileStreaming_clearsUiStateAndResetsSessionImmediately() = runTest {
        val streamGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val fakeRepo = object : GeminiRepository {
            var resetCalled = false
            override suspend fun generateText(prompt: String) = Result.success("Static")
            override fun sendMessageStream(prompt: String): Flow<String> = flow {
                emit("First chunk emitted")
                streamGate.await()
                emit("Second chunk after gate")
            }
            override fun resetChatSession() {
                resetCalled = true
            }
        }

        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        testScheduler.runCurrent()

        viewModel.onPromptChange("Streaming prompt")
        viewModel.onSend()
        testScheduler.runCurrent()

        // Verify first chunk has arrived and assistant message is pending
        val activeState = viewModel.uiState.value
        assertTrue(activeState.isLoading)
        assertEquals(2, activeState.messages.size)
        assertEquals("First chunk emitted", activeState.messages[1].text)
        assertTrue(activeState.messages[1].isPending)

        // Clear chat while stream is still awaiting the gate
        viewModel.onClearChat()
        testScheduler.runCurrent()

        // State must be completely cleared immediately
        val clearedState = viewModel.uiState.value
        assertTrue(clearedState.messages.isEmpty())
        assertTrue(clearedState.response.isEmpty())
        assertTrue(fakeRepo.resetCalled)

        // Unblock gate to allow background collection to finish
        streamGate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun onSend_streaming_observesIncrementalTokenAndPendingStateProgression() = runTest {
        val step1 = kotlinx.coroutines.CompletableDeferred<Unit>()
        val step2 = kotlinx.coroutines.CompletableDeferred<Unit>()
        val fakeRepo = object : GeminiRepository {
            override suspend fun generateText(prompt: String) = Result.success("Static")
            override fun sendMessageStream(prompt: String): Flow<String> = flow {
                emit("Part 1")
                step1.await()
                emit(" Part 2")
                step2.await()
            }
            override fun resetChatSession() {}
        }

        val viewModel = ChatViewModel(
            repository = fakeRepo,
            hasApiKey = true,
            chatHistoryRepository = historyRepository,
        )
        testScheduler.runCurrent()

        viewModel.onPromptChange("Step stream")
        viewModel.onSend()
        testScheduler.runCurrent()

        // Observation 1: Part 1 received, isPending == true, isLoading == true
        val state1 = viewModel.uiState.value
        assertTrue(state1.isLoading)
        assertEquals(2, state1.messages.size)
        assertEquals("Part 1", state1.messages[1].text)
        assertTrue(state1.messages[1].isPending)

        // Advance to Observation 2: Part 2 received
        step1.complete(Unit)
        testScheduler.runCurrent()
        val state2 = viewModel.uiState.value
        assertTrue(state2.isLoading)
        assertEquals("Part 1 Part 2", state2.messages[1].text)
        assertTrue(state2.messages[1].isPending)

        // Complete the stream
        step2.complete(Unit)
        advanceUntilIdle()

        // Final observation: isPending == false, isLoading == false
        val finalState = viewModel.uiState.value
        assertFalse(finalState.isLoading)
        assertEquals("Part 1 Part 2", finalState.messages[1].text)
        assertFalse(finalState.messages[1].isPending)
    }
}
