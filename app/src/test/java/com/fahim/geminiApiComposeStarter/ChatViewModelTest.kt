package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.MessageSender
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onPromptChange_updatesPromptState() {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_emptyPrompt_setsEmptyPromptError() {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = false)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun onSend_successfulResponse_appendsMessagesAndStopsLoading() = runTest(testDispatcher) {
        fakeRepository.responseToReturn = Result.success("Hello from Gemini AI!")
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)

        viewModel.onPromptChange("Tell me a joke")
        viewModel.onSend()

        // Before coroutines finish
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)

        advanceUntilIdle()

        // After completion
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Tell me a joke", messages[0].text)
        assertEquals(MessageSender.USER, messages[0].sender)
        assertEquals("Hello from Gemini AI!", messages[1].text)
        assertEquals(MessageSender.GEMINI, messages[1].sender)
    }

    @Test
    fun onSend_failureResponse_setsErrorMessageAndStopsLoading() = runTest(testDispatcher) {
        fakeRepository.responseToReturn = Result.failure(RuntimeException("Network timeout"))
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)

        viewModel.onPromptChange("What is Quantum Physics?")
        viewModel.onSend()

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Network timeout"))
    }

    @Test
    fun onClearChat_removesAllMessages() = runTest(testDispatcher) {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)
        fakeDao.insertMessage(ChatMessageEntity(text = "Sample", sender = MessageSender.USER))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)

        viewModel.onClearChat()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.messages.size)
    }
}

class FakeGeminiRepository : GeminiRepository {
    var responseToReturn: Result<String> = Result.success("Default response")

    override suspend fun generateText(prompt: String): Result<String> {
        return responseToReturn
    }
}

class FakeChatDao : ChatDao {
    private val messages = mutableListOf<ChatMessageEntity>()
    private val _flow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _flow.asStateFlow()

    override suspend fun insertMessage(message: ChatMessageEntity): Long {
        messages.add(message)
        _flow.value = messages.toList()
        return messages.size.toLong()
    }

    override suspend fun clearAllMessages() {
        messages.clear()
        _flow.value = emptyList()
    }

    override suspend fun deleteMessage(id: Long) {
        messages.removeAll { it.id == id }
        _flow.value = messages.toList()
    }
}
