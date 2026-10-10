package com.fahim.geminiApiComposeStarter.ui.chat

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `initial state is empty and not loading`() = runTest {
        val repo = FakeGeminiRepository()
        val vm = ChatViewModel(repo, hasApiKey = true)
        val state = vm.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPromptChange updates prompt in state`() = runTest {
        val vm = ChatViewModel(FakeGeminiRepository(), hasApiKey = true)
        vm.onPromptChange("Explain DRS")
        assertEquals("Explain DRS", vm.uiState.value.prompt)
    }

    @Test
    fun `onSend with empty prompt sets promptError`() = runTest {
        val vm = ChatViewModel(FakeGeminiRepository(), hasApiKey = true)
        vm.onSend()
        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertEquals(0, (FakeGeminiRepository()).generateCallCount)
    }

    @Test
    fun `onSend with no api key sets errorMessage`() = runTest {
        val vm = ChatViewModel(FakeGeminiRepository(), hasApiKey = false)
        vm.onPromptChange("Explain DRS")
        vm.onSend()
        assertNotNull(vm.uiState.value.errorMessage)
        assertTrue(vm.uiState.value.errorMessage!!.contains("GEMINI_API_KEY"))
    }

    @Test
    fun `onSend success path adds user and bot messages`() = runTest {
        val fakeResponse = "Copy that. DRS is Drag Reduction System."
        val repo = FakeGeminiRepository(Result.success(fakeResponse))
        val vm = ChatViewModel(repo, hasApiKey = true)

        vm.onPromptChange("Explain DRS")
        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)

        // Check user message in list
        val userMsg = state.messages.find { it.sender == "DRIVER" }
        assertNotNull(userMsg)
        assertEquals("Explain DRS", userMsg!!.text)

        // Check bot message in list
        val botMsg = state.messages.find { it.sender == "PITWALL" && !it.isLoading }
        assertNotNull(botMsg)
        assertEquals(fakeResponse, botMsg!!.text)
    }

    @Test
    fun `onSend error path sets errorMessage`() = runTest {
        val repo = FakeGeminiRepository(Result.failure(RuntimeException("RADIO FAILURE")))
        val vm = ChatViewModel(repo, hasApiKey = true)

        vm.onPromptChange("Explain DRS")
        vm.onSend()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("RADIO FAILURE"))
    }

    @Test
    fun `loading state is true while waiting for response`() = runTest {
        val repo = FakeGeminiRepository().apply { shouldDelayResponse = true }
        val vm = ChatViewModel(repo, hasApiKey = true)

        vm.onPromptChange("Test prompt")
        vm.onSend()
        testDispatcher.scheduler.runCurrent()

        // Before advanceUntilIdle, isLoading should be true
        assertTrue(vm.uiState.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `onSend while loading is ignored`() = runTest {
        val repo = FakeGeminiRepository().apply { shouldDelayResponse = true }
        val vm = ChatViewModel(repo, hasApiKey = true)

        vm.onPromptChange("First")
        vm.onSend()
        testDispatcher.scheduler.runCurrent()

        // Try to send again while loading - should be ignored
        vm.onPromptChange("Second")
        vm.onSend()
        advanceUntilIdle()

        // Only 1 call should have been made
        assertEquals(1, repo.generateCallCount)
    }

    @Test
    fun `onClearChat removes all messages`() = runTest {
        val repo = FakeGeminiRepository()
        val vm = ChatViewModel(repo, hasApiKey = true)

        vm.onPromptChange("Explain DRS")
        vm.onSend()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.messages.isNotEmpty())

        vm.onClearChat()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.messages.isEmpty())
    }

    @Test
    fun `onChipSelected sends chip text as message`() = runTest {
        val repo = FakeGeminiRepository()
        val vm = ChatViewModel(repo, hasApiKey = true)

        vm.onChipSelected("Tyre strategy basics")
        advanceUntilIdle()

        assertEquals("Tyre strategy basics", repo.lastPrompt)
        val userMsg = vm.uiState.value.messages.find { it.sender == "DRIVER" }
        assertEquals("Tyre strategy basics", userMsg?.text)
    }
}
