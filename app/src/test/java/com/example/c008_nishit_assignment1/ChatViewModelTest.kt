package com.example.c008_nishit_assignment1

import com.example.c008_nishit_assignment1.data.local.ChatDao
import com.example.c008_nishit_assignment1.data.local.ChatMessageEntity
import com.example.c008_nishit_assignment1.data.preferences.PreferencesRepository
import com.example.c008_nishit_assignment1.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class FakeChatDao : ChatDao {
    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    override fun getAllMessages(): Flow<List<ChatMessageEntity>> = _messages

    override suspend fun insertMessage(message: ChatMessageEntity): Long {
        val currentList = _messages.value.toMutableList()
        val id = (currentList.size + 1).toLong()
        currentList.add(message.copy(id = id))
        _messages.value = currentList
        return id
    }

    override suspend fun clearAllMessages() {
        _messages.value = emptyList()
    }
}

class FakePreferencesRepository : PreferencesRepository(null) {
    private val _isDarkMode = MutableStateFlow<Boolean?>(null)

    override val isDarkMode: Flow<Boolean?> = _isDarkMode

    override suspend fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeChatDao: FakeChatDao
    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeChatDao = FakeChatDao()
        fakePreferencesRepository = FakePreferencesRepository()
        viewModel = ChatViewModel(fakeRepository, fakeChatDao, fakePreferencesRepository, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendPrompt_emptyPrompt_setsErrorMessage() = runTest {
        viewModel.sendPrompt("")
        advanceUntilIdle()

        assertEquals("Prompt cannot be empty.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun sendPrompt_success_savesUserAndGeminiMessage() = runTest {
        fakeRepository.responseToReturn = "Hello from Gemini!"

        viewModel.sendPrompt("Hi")
        advanceUntilIdle()

        val messages = viewModel.uiState.value.messages
        assertEquals(2, messages.size)
        assertEquals("Hi", messages[0].text)
        assertEquals(true, messages[0].isUser)
        assertEquals("Hello from Gemini!", messages[1].text)
        assertEquals(false, messages[1].isUser)
    }

    @Test
    fun sendPrompt_failure_setsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        fakeRepository.errorToReturn = Exception("Network Connection Failed")

        viewModel.sendPrompt("Hi Gemini")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals("Network Connection Failed", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearHistory_removesAllMessages() = runTest {
        fakeRepository.responseToReturn = "Response"
        viewModel.sendPrompt("Message")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearHistory()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.messages.size)
    }
}
