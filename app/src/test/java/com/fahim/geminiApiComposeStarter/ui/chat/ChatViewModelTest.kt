package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.data.preferences.ThemeMode
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

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
    fun `empty prompt sets promptError`() {
        val fakeRepo = FakeGeminiRepository(Result.success("Success"))
        val viewModel = ChatViewModel(fakeRepo, hasApiKey = true)

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `missing api key sets errorMessage`() {
        val fakeRepo = FakeGeminiRepository(Result.success("Success"))
        val viewModel = ChatViewModel(fakeRepo, hasApiKey = false)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `voice result populates prompt without auto-sending`() {
        val fakeRepo = FakeGeminiRepository(Result.success("Success"))
        val viewModel = ChatViewModel(fakeRepo, hasApiKey = true)

        viewModel.onVoiceResult("Voice Recognized Text")

        assertEquals("Voice Recognized Text", viewModel.uiState.value.prompt)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `theme mode change updates state`() {
        val fakeRepo = FakeGeminiRepository(Result.success("Success"))
        val viewModel = ChatViewModel(fakeRepo, hasApiKey = true)

        viewModel.onThemeModeChange(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `successful generateText updates response and saves messages`() = runTest {
        val fakeRepo = FakeGeminiRepository(Result.success("Hello world response"))
        val viewModel = ChatViewModel(fakeRepo, hasApiKey = true)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Hello world response", viewModel.uiState.value.response)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(2, fakeRepo.savedMessages.size)
        assertEquals("user", fakeRepo.savedMessages[0].sender)
        assertEquals("model", fakeRepo.savedMessages[1].sender)
    }

    @Test
    fun `failed generateText updates errorMessage`() = runTest {
        val fakeRepo = FakeGeminiRepository(Result.failure(RuntimeException("Network error")))
        val viewModel = ChatViewModel(fakeRepo, hasApiKey = true)

        viewModel.onPromptChange("Hello")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeGeminiRepository(private val result: Result<String>) : GeminiRepository {
        val savedMessages = mutableListOf<ChatMessageEntity>()
        private val historyFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

        override fun getChatHistory(): Flow<List<ChatMessageEntity>> = historyFlow

        override suspend fun saveMessage(sender: String, content: String) {
            val entity = ChatMessageEntity(id = (savedMessages.size + 1).toLong(), sender = sender, content = content)
            savedMessages.add(entity)
            historyFlow.value = savedMessages.toList()
        }

        override suspend fun clearHistory() {
            savedMessages.clear()
            historyFlow.value = emptyList()
        }

        override suspend fun generateText(prompt: String): Result<String> = result
    }
}
