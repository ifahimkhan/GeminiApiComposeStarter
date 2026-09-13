package com.fahim.geminiApiComposeStarter.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDao : ChatMessageDao {

        private val messages = mutableListOf<ChatMessageEntity>()

        override suspend fun insertMessage(message: ChatMessageEntity) {
            messages.add(message)
        }

        override fun getAllMessages(): Flow<List<ChatMessageEntity>> {
            return flowOf(messages)
        }

        override suspend fun deleteAllMessages() {
            messages.clear()
        }
    }

    private class FakeGeminiRepository : GeminiRepository {

        override suspend fun generateText(prompt: String): Result<String> {
            return Result.success("Fake response")
        }
    }

    private fun createViewModel(): ChatViewModel {
        val dao = FakeDao()
        val historyRepository = ChatHistoryRepository(dao)

        return ChatViewModel(
            repository = FakeGeminiRepository(),
            hasApiKey = true,
            historyRepository = historyRepository
        )
    }

    @Test
    fun onPromptChange_updatesPrompt() = runTest {
        val viewModel = createViewModel()

        viewModel.onPromptChange("Hello Gemini")

        assertEquals(
            "Hello Gemini",
            viewModel.uiState.value.prompt
        )
    }

    @Test
    fun emptyPrompt_showsError() = runTest {
        val viewModel = createViewModel()

        viewModel.onSend()

        assertEquals(
            PromptError.EMPTY,
            viewModel.uiState.value.promptError
        )
    }
}