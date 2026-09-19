package com.nmims.c010.gemini.ui.chat

import app.cash.turbine.test
import com.nmims.c010.gemini.data.Author
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: FakeGeminiRepository
    private lateinit var dao: FakeChatDao
    private lateinit var vm: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = FakeGeminiRepository()
        dao = FakeChatDao()
        vm = ChatViewModel(repo, dao)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `send appends the user message immediately and clears the field`() = runTest {
        vm.onInputChange("Hi")
        vm.send()

        val state = vm.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals(Author.USER, state.messages.first().author)
        assertEquals("", state.input)
    }

    @Test
    fun `successful reply appends a model message and clears loading`() = runTest {
        vm.uiState.test {
            awaitItem()
            vm.onInputChange("Hi")
            awaitItem()
            vm.send()
            assertTrue(awaitItem().isLoading)

            advanceUntilIdle()
            val done = expectMostRecentItem()
            assertFalse(done.isLoading)
            assertEquals(Author.MODEL, done.messages.last().author)
            assertEquals("Hello from Gemini", done.messages.last().text)
        }
    }

    @Test
    fun `repository failure surfaces an error message`() = runTest {
        repo.failWith(IOException("No internet"))
        vm.onInputChange("Hi")
        vm.send()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("No internet", state.errorMessage)
    }

    @Test
    fun `blank input is ignored`() = runTest {
        vm.onInputChange("   ")
        vm.send()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.messages.isEmpty())
        assertNull(repo.lastPrompt)
    }

    @Test
    fun `consumeError clears the error state`() = runTest {
        repo.failWith(RuntimeException("boom"))
        vm.onInputChange("Hi")
        vm.send()
        advanceUntilIdle()

        vm.consumeError()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `voice result is put into the input field`() = runTest {
        vm.onVoiceResult("what is jetpack compose")
        assertEquals("what is jetpack compose", vm.uiState.value.input)
        assertFalse(vm.uiState.value.isListening)
    }
}
