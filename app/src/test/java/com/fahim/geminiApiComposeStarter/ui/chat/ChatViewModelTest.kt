package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * Unit tests for ChatViewModel using kotlinx-coroutines-test and a fake GeminiRepository.
 */
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

    @Test
    fun onPromptChange_updatesPromptAndClearsError() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, ioDispatcher = testDispatcher)

        viewModel.onPromptChange("Hello World")
        assertEquals("Hello World", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withEmptyPrompt_setsPromptError() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, ioDispatcher = testDispatcher)

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = false)

        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        assertEquals(
            ChatViewModel.MISSING_API_KEY_MESSAGE,
            viewModel.uiState.value.errorMessage
        )
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_successfulResponse_addsMessagesToState() = runTest(testDispatcher) {
        val expectedResponse = "Hello! How can I assist you today?"
        val fakeRepo = FakeGeminiRepository(Result.success(expectedResponse))
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, ioDispatcher = testDispatcher)

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(2, state.messages.size)

        // Verify User message
        assertEquals("Hi there", state.messages[0].text)
        assertTrue(state.messages[0].isFromUser)

        // Verify Gemini response message
        assertEquals(expectedResponse, state.messages[1].text)
        assertFalse(state.messages[1].isFromUser)
    }

    @Test
    fun onSend_failureResponse_updatesErrorMessage() = runTest(testDispatcher) {
        val errorMessage = "Quota exceeded or network timeout"
        val fakeRepo = FakeGeminiRepository(Result.failure(RuntimeException(errorMessage)))
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, ioDispatcher = testDispatcher)

        viewModel.onPromptChange("Generate story")
        viewModel.onSend()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(errorMessage, state.errorMessage)
    }

    @Test
    fun onVoiceInputResult_updatesPrompt() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository()
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, ioDispatcher = testDispatcher)

        viewModel.onVoiceInputResult("Voice recognized question")
        assertEquals("Voice recognized question", viewModel.uiState.value.prompt)
    }

    @Test
    fun onClearHistory_clearsAllMessages() = runTest(testDispatcher) {
        val fakeRepo = FakeGeminiRepository(Result.success("Answer"))
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, ioDispatcher = testDispatcher)

        viewModel.onPromptChange("Question")
        viewModel.onSend()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.onClearHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun withChatDao_persistsAndLoadsMessages() = runTest(testDispatcher) {
        val fakeDao = FakeChatDao()
        val fakeRepo = FakeGeminiRepository(Result.success("Persisted Answer"))
        val viewModel = ChatViewModel(repository = fakeRepo, hasApiKey = true, chatDao = fakeDao, ioDispatcher = testDispatcher)

        viewModel.onPromptChange("Persisted Question")
        viewModel.onSend()
        advanceUntilIdle()

        val savedEntities = fakeDao.entities
        assertEquals(2, savedEntities.size)
        assertEquals("Persisted Question", savedEntities[0].text)
        assertEquals("Persisted Answer", savedEntities[1].text)
    }
}

/**
 * Fake GeminiRepository for isolated, deterministic unit testing.
 */
class FakeGeminiRepository(
    private val result: Result<String> = Result.success("Default response")
) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> = result
}

/**
 * In-memory fake ChatDao implementation for testing Room integration.
 */
class FakeChatDao : ChatDao {
    val entities = mutableListOf<ChatMessageEntity>()
    private val flow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = flow.asStateFlow()

    override fun insertMessage(message: ChatMessageEntity): Long {
        val id = (entities.size + 1).toLong()
        val saved = message.copy(id = id)
        entities.add(saved)
        flow.value = entities.toList()
        return id
    }

    override fun clearAllMessages(): Int {
        val count = entities.size
        entities.clear()
        flow.value = emptyList()
        return count
    }

    override fun deleteMessageById(id: Long): Int {
        val before = entities.size
        entities.removeAll { it.id == id }
        flow.value = entities.toList()
        return before - entities.size
    }
}
