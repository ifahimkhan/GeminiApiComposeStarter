package com.example.c031_geminiapicompose.ui.chat

import com.example.c031_geminiapicompose.MainDispatcherRule
import com.example.c031_geminiapicompose.data.FakeGeminiRepository
import com.example.c031_geminiapicompose.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeUserPreferencesRepository: UserPreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeGeminiRepository()
        fakeUserPreferencesRepository = UserPreferencesRepository()
        viewModel = ChatViewModel(
            repository = fakeRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true
        )
    }

    @Test
    fun initialState_isEmpty() {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.promptError)
    }

    @Test
    fun onPromptChange_updatesPrompt() {
        viewModel.onPromptChange("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_emptyPrompt_setsPromptError() {
        viewModel.onPromptChange("")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_withoutApiKey_setsErrorMessage() {
        val noKeyViewModel = ChatViewModel(
            repository = fakeRepository,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = false
        )
        noKeyViewModel.onPromptChange("Hello")
        noKeyViewModel.onSend()
        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, noKeyViewModel.uiState.value.errorMessage)
    }

    @Test
    fun onSend_validPrompt_successUpdatesMessages() = runTest {
        viewModel.onPromptChange("What is Kotlin?")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt) // Prompt cleared after send
        assertEquals(2, state.messages.size)
        assertEquals("What is Kotlin?", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Hello from fake Gemini!", state.messages[1].text)
        assertFalse(state.messages[1].isUser)
        assertFalse(state.isLoading)
    }

    @Test
    fun onSend_failure_setsErrorMessage() = runTest {
        val failingRepo = FakeGeminiRepository(shouldFail = true)
        val failingViewModel = ChatViewModel(
            repository = failingRepo,
            userPreferencesRepository = fakeUserPreferencesRepository,
            hasApiKey = true
        )
        failingViewModel.onPromptChange("Fail test")
        failingViewModel.onSend()

        val state = failingViewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Fake API Network Error", state.errorMessage)
    }

    @Test
    fun clearHistory_removesAllMessages() = runTest {
        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearHistory()
        assertEquals(0, viewModel.uiState.value.messages.size)
    }
}
