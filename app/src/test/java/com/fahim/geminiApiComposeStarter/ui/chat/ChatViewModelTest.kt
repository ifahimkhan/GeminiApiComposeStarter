package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatEntity
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeGeminiRepository : GeminiRepository {
    private val _history = MutableStateFlow<List<ChatEntity>>(emptyList())
    var shouldFail = false

    override suspend fun generateText(prompt: String): Result<String> {
        return if (shouldFail) {
            Result.failure(Exception("Mock Error"))
        } else {
            Result.success("Response to: $prompt")
        }
    }

    override fun getChatHistory(): Flow<List<ChatEntity>> = _history

    override suspend fun saveMessage(role: String, content: String) {
        val newList = _history.value.toMutableList()
        newList.add(ChatEntity(id = newList.size, role = role, content = content))
        _history.value = newList
    }

    override suspend fun clearHistory() {
        _history.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSend success updates history`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        
        viewModel.onSend("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("user", state.messages[0].role)
        assertEquals("Hello", state.messages[0].content)
        assertEquals("model", state.messages[1].role)
    }

    @Test
    fun `onSend failure sets error`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }

        fakeRepository.shouldFail = true
        viewModel.onSend("Hello")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size) // Only user message
        assertEquals("Mock Error", state.error)
    }
}
