package com.fahim.geminiApiComposeStarter

import com.fahim.geminiApiComposeStarter.data.ChatHistory
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.data.ThemePreferences
import com.fahim.geminiApiComposeStarter.ui.chat.ChatViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun sendingMessageSavesBothSidesAndClearsLoading() = runTest(dispatcher) {
        val history = FakeHistory()
        val viewModel = ChatViewModel(FakeGemini(Result.success("Hello back")), history, FakePreferences(), true)
        viewModel.onPromptChange("Hello")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(listOf("Hello", "Hello back"), viewModel.uiState.value.messages.map { it.text })
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("", viewModel.uiState.value.prompt)
    }

    @Test fun blankPromptShowsValidationError() = runTest(dispatcher) {
        val viewModel = ChatViewModel(FakeGemini(Result.success("Unused")), FakeHistory(), FakePreferences(), true)
        viewModel.onSend()
        assertTrue(viewModel.uiState.value.promptError)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test fun apiFailurePreservesUserMessageAndShowsError() = runTest(dispatcher) {
        val viewModel = ChatViewModel(
            FakeGemini(Result.failure(IllegalStateException("Network unavailable"))),
            FakeHistory(), FakePreferences(), true
        )
        viewModel.onPromptChange("Question")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(listOf("Question"), viewModel.uiState.value.messages.map { it.text })
        assertEquals("Could not get a Gemini response. Check your connection and key.", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test fun historyAndThemeAreRestored() = runTest(dispatcher) {
        val history = FakeHistory()
        history.save(ChatMessage("saved", "Earlier message", true))
        val preferences = FakePreferences()
        preferences.setMode(ThemeMode.DARK)
        val viewModel = ChatViewModel(FakeGemini(Result.success("Unused")), history, preferences, false)
        advanceUntilIdle()
        assertEquals("Earlier message", viewModel.uiState.value.messages.single().text)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    private class FakeGemini(private val result: Result<String>) : GeminiRepository {
        override suspend fun generateText(prompt: String) = result
    }

    private class FakeHistory : ChatHistory {
        private val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        override fun observeAll(): Flow<List<ChatMessage>> = messages
        override suspend fun save(message: ChatMessage) { messages.value = messages.value + message }
        override suspend fun clear() { messages.value = emptyList() }
    }

    private class FakePreferences : ThemePreferences {
        private val state = MutableStateFlow(ThemeMode.SYSTEM)
        override val mode: Flow<ThemeMode> = state
        override suspend fun setMode(mode: ThemeMode) { state.value = mode }
    }
}
