package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.ChatDao
import com.fahim.geminiApiComposeStarter.data.ChatEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeDao = object : ChatDao {
        val messages = mutableListOf<ChatEntity>()
        override fun getAllMessages(): Flow<List<ChatEntity>> = flowOf(messages)
        override suspend fun insertMessage(message: ChatEntity) { messages.add(message) }
        override suspend fun clearChat() { messages.clear() }
    }

    private val fakeRepository = object : GeminiRepository {
        override suspend fun generateText(prompt: String): Result<String> {
            return Result.success("Echo: $prompt")
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
    }

    @Test
    fun onSend_emptyPrompt_setsError() {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_validPrompt_insertsMessageAndClearsPrompt() = runTest {
        val viewModel = ChatViewModel(fakeRepository, fakeDao, hasApiKey = true)
        viewModel.onPromptChange("Test query")
        viewModel.onSend()
        testDispatcher.scheduler.advanceUntilIdle()

        // Prompt should be cleared after sending
        assertEquals("", viewModel.uiState.value.prompt)
        // Message pairs should be stored in DAO
        assertEquals(2, fakeDao.messages.size)
    }
}