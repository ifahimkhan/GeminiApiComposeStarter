package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatDao
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.local.ThemeManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val repository: GeminiRepository = mockk()
    private val chatDao: ChatDao = mockk(relaxed = true)
    private val themeManager: ThemeManager = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { chatDao.getAllMessages() } returns flowOf(emptyList())
        coEvery { themeManager.isDarkMode } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSend updates state with user message and calls repository`() = runTest {
        val viewModel = ChatViewModel(repository, chatDao, themeManager, true)
        val prompt = "Hello"
        viewModel.onPromptChange(prompt)

        coEvery { repository.generateText(any(), any()) } returns Result.success("Hi there")

        viewModel.onSend()

        assertEquals("", viewModel.uiState.value.prompt)
        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals(prompt, viewModel.uiState.value.messages[0].text)

        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("Hi there", viewModel.uiState.value.messages[1].text)
        coVerify { chatDao.insertMessage(any()) }
    }

    @Test
    fun `loadChatHistory populates messages from dao`() = runTest {
        val savedMessages = listOf(
            ChatMessageEntity(uid = 1, text = "Old message", isFromUser = true)
        )
        coEvery { chatDao.getAllMessages() } returns flowOf(savedMessages)

        val viewModel = ChatViewModel(repository, chatDao, themeManager, true)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.messages.size)
        assertEquals("Old message", viewModel.uiState.value.messages[0].text)
    }
}
