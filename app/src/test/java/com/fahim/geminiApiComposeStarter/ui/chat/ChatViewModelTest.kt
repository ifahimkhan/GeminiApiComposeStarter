package com.fahim.geminiApiComposeStarter.ui.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ChatViewModel
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var fakeUserPrefs: FakeUserPreferencesRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        fakeUserPrefs = FakeUserPreferencesRepository()
        viewModel = ChatViewModel(fakeRepository, fakeDao, fakeUserPrefs, hasApiKey = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun onPromptChange_updatesPrompt() {
        viewModel.onPromptChange("Hello")
        assertEquals("Hello", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_emptyPrompt_setsError() {
        viewModel.onPromptChange("   ")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun onSend_success_updatesMessages() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()
        
        // Wait for coroutines to complete
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertTrue(state.messages[0].isUser)
        assertEquals("Hello Gemini", state.messages[0].text)
        assertFalse(state.messages[1].isUser)
        assertEquals("Fake Gemini Response", state.messages[1].text)
    }
}