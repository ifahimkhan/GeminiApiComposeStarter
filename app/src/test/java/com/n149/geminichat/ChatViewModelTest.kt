package com.n149.geminichat

import com.n149.geminichat.data.GeminiRepositoryInterface
import com.n149.geminichat.data.MessageDao
import com.n149.geminichat.data.MessageEntity
import com.n149.geminichat.ui.chat.ChatViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ChatViewModel].
 *
 * Uses a [TestCoroutineDispatcher] so all coroutine work runs synchronously.
 * [GeminiRepositoryInterface] and [MessageDao] are mocked with MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @MockK
    lateinit var repository: GeminiRepositoryInterface

    @MockK
    lateinit var messageDao: MessageDao

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChatViewModel

    private val messagesFlow = MutableStateFlow<List<MessageEntity>>(emptyList())

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        every { messageDao.getAllMessages() } returns messagesFlow
        viewModel = ChatViewModel(repository, messageDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state has empty messages and no loading`() {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onInputChanged updates inputText`() {
        viewModel.onInputChanged("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage persists user message and clears input`() = runTest {
        coEvery { messageDao.insertMessage(any()) } returns 1L
        coEvery { messageDao.getRecentMessages(any()) } returns emptyList()
        coEvery { repository.sendMessage(any(), any()) } returns "Hello there!"

        viewModel.onInputChanged("Hi")
        viewModel.sendMessage()
        advanceUntilIdle()

        coVerify { messageDao.insertMessage(match { it.role == "user" && it.content == "Hi" }) }
        assertEquals("", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendMessage saves model reply on success`() = runTest {
        coEvery { messageDao.insertMessage(any()) } returns 1L
        coEvery { messageDao.getRecentMessages(any()) } returns emptyList()
        coEvery { repository.sendMessage(any(), "test") } returns "Gemini reply"

        viewModel.sendMessage("test")
        advanceUntilIdle()

        coVerify { messageDao.insertMessage(match { it.role == "model" && it.content == "Gemini reply" }) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `sendMessage sets errorMessage on failure`() = runTest {
        coEvery { messageDao.insertMessage(any()) } returns 1L
        coEvery { messageDao.getRecentMessages(any()) } returns emptyList()
        coEvery { repository.sendMessage(any(), any()) } throws RuntimeException("Network error")

        viewModel.sendMessage("test")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Network error"))
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `dismissError clears errorMessage`() = runTest {
        coEvery { messageDao.insertMessage(any()) } returns 1L
        coEvery { messageDao.getRecentMessages(any()) } returns emptyList()
        coEvery { repository.sendMessage(any(), any()) } throws RuntimeException("err")

        viewModel.sendMessage("test")
        advanceUntilIdle()

        viewModel.dismissError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sendMessage does nothing when input is blank`() = runTest {
        viewModel.sendMessage("   ")
        advanceUntilIdle()
        coVerify(exactly = 0) { messageDao.insertMessage(any()) }
    }

    @Test
    fun `onVoiceResult sets input and triggers send`() = runTest {
        coEvery { messageDao.insertMessage(any()) } returns 1L
        coEvery { messageDao.getRecentMessages(any()) } returns emptyList()
        coEvery { repository.sendMessage(any(), "voice text") } returns "reply"

        viewModel.onVoiceResult("voice text")
        advanceUntilIdle()

        coVerify { repository.sendMessage(any(), "voice text") }
        assertFalse(viewModel.uiState.value.isListening)
    }

    @Test
    fun `clearHistory calls dao clearAll`() = runTest {
        coEvery { messageDao.clearAll() } just Runs
        viewModel.clearHistory()
        advanceUntilIdle()
        coVerify { messageDao.clearAll() }
    }
}
