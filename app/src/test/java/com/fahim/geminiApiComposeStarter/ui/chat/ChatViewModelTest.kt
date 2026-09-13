package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeGeminiRepository("Fake Gemini reply")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendPrompt_updatesStateWithUserMessageAndFakeReply() = runTest {
        val viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true)

        viewModel.onPromptChange("Hello Gemini")
        viewModel.onSend()

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("Hello Gemini", state.messages[0].text)
        assertEquals(true, state.messages[0].isUser)
        assertEquals("Fake Gemini reply", state.messages[1].text)
        assertEquals(false, state.messages[1].isUser)
    }
}

private class FakeGeminiRepository(private val response: String) : GeminiRepository {
    override suspend fun generateText(prompt: String): Result<String> {
        return Result.success(response)
    }
}
