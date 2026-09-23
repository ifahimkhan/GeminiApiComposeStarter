package com.example.n075nishantassgn1

import com.example.n075nishantassgn1.data.GeminiRepository
import com.example.n075nishantassgn1.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ChatViewModel

    private class FakeGeminiRepository : GeminiRepository {

        override suspend fun sendMessage(message: String): String {
            return "Fake Gemini response"
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        viewModel = ChatViewModel(
            repository = FakeGeminiRepository(),
            historyDao = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sendMessage_addsUserAndGeminiMessages() = runTest {

        viewModel.sendMessage("Hello Gemini")

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(2, state.messages.size)

        assertEquals(
            "Hello Gemini",
            state.messages[0].text
        )

        assertTrue(state.messages[0].isUser)

        assertEquals(
            "Fake Gemini response",
            state.messages[1].text
        )

        assertFalse(state.messages[1].isUser)

        assertFalse(state.isLoading)
    }

    @Test
    fun blankMessage_isIgnored() = runTest {

        viewModel.sendMessage("")

        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
    }
}