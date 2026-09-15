package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Ensure this matches your ViewModel constructor
        viewModel = ChatViewModel(FakeGeminiRepository(), FakeChatDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
    }
}

// Ensure these fakes are at the bottom of the file
class FakeGeminiRepository : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> = Result.success("Success")
}

class FakeChatDao : ChatDao {
    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messages
    override suspend fun insertMessage(message: ChatMessageEntity) {
        _messages.value = _messages.value + message
    }
}