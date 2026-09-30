package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.db.ChatDao
import com.fahim.geminiApiComposeStarter.data.db.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

/** Fake repository for unit tests. */
class FakeGeminiRepository : GeminiRepository {
    var response: Result<String> = Result.success("Fake response")
    override suspend fun generateText(prompt: String): Result<String> = response
}

/** Fake DAO that stores messages in a MutableStateFlow for easy inspection. */
class FakeChatDao : ChatDao {
    val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = messagesFlow

    override suspend fun insertMessage(message: ChatMessageEntity) {
        messagesFlow.value = messagesFlow.value + message
    }

    override suspend fun clearHistory() {
        messagesFlow.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChatViewModel
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatDao()
        viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onPromptChange_updatesPrompt() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello")
        assertEquals("Hello", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_emptyPrompt_setsError() = runTest(testDispatcher) {
        viewModel.onPromptChange("")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun onPromptChange_clearsPreviousError() = runTest(testDispatcher) {
        viewModel.onSend() // triggers EMPTY error
        viewModel.onPromptChange("Fixed text")
        assertNull(viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_missingApiKey_setsErrorMessage() = runTest(testDispatcher) {
        val vmNoKey = ChatViewModel(fakeRepository, fakeDao, hasApiKey = false)
        vmNoKey.onPromptChange("Hello")
        vmNoKey.onSend()
        assertNotNull(vmNoKey.uiState.value.errorMessage)
        assertFalse(vmNoKey.uiState.value.isLoading)
    }

    @Test
    fun onSend_validPrompt_persistsTwoMessages() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()
        testScheduler.advanceUntilIdle()

        val messages = fakeDao.messagesFlow.value
        assertEquals(2, messages.size)
        assertTrue(messages[0].isUser)
        assertEquals("Hello Gemini", messages[0].text)
        assertFalse(messages[1].isUser)
        assertEquals("Fake response", messages[1].text)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun clearHistory_emptiesDao() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        testScheduler.advanceUntilIdle()

        viewModel.clearHistory()
        testScheduler.advanceUntilIdle()

        assertTrue(fakeDao.messagesFlow.value.isEmpty())
    }
}
