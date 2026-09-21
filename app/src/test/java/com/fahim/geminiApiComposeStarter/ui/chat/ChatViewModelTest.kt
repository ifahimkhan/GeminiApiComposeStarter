package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.data.ChatHistoryRepository
import com.fahim.geminiApiComposeStarter.fakes.FakeChatMessageDao
import com.fahim.geminiApiComposeStarter.fakes.FakeGeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var historyRepository: ChatHistoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeRepository = FakeGeminiRepository()
        historyRepository = ChatHistoryRepository(FakeChatMessageDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(hasApiKey: Boolean = true) =
        ChatViewModel(fakeRepository, historyRepository, hasApiKey)

    @Test
    fun `sending an empty prompt sets a validation error and does not call the repository`() = runTest {
        val vm = viewModel()
        vm.onPromptChange("   ")

        vm.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PromptError.EMPTY, vm.uiState.value.promptError)
        assertNull(fakeRepository.lastPrompt)
    }

    @Test
    fun `sending without an api key surfaces the missing key message`() = runTest {
        val vm = viewModel(hasApiKey = false)
        vm.onPromptChange("Hello")

        vm.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, vm.uiState.value.errorMessage)
        assertNull(fakeRepository.lastPrompt)
    }

    @Test
    fun `successful send appends user and model turns and clears the prompt`() = runTest {
        fakeRepository.nextResult = Result.success("**Hi** there")
        val vm = viewModel()
        vm.onPromptChange("Hello Gemini")

        vm.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("", state.prompt)
        assertEquals(false, state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("Hello Gemini", state.messages[0].text)
        assertEquals(ChatRole.MODEL, state.messages[1].role)
        assertEquals("**Hi** there", state.messages[1].text)
    }

    @Test
    fun `failed send surfaces the error message and still records the user turn`() = runTest {
        fakeRepository.nextResult = Result.failure(IllegalStateException("network down"))
        val vm = viewModel()
        vm.onPromptChange("Hello Gemini")

        vm.onSend()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("network down", state.errorMessage)
        assertEquals(false, state.isLoading)
        assertTrue(state.messages.any { it.role == ChatRole.USER && it.text == "Hello Gemini" })
        assertEquals(1, state.messages.size)
    }

    @Test
    fun `voice result fills the prompt and clears listening state`() {
        val vm = viewModel()
        vm.onListeningChange(true)

        vm.onVoiceResult("what's the weather today")

        val state = vm.uiState.value
        assertEquals("what's the weather today", state.prompt)
        assertEquals(false, state.isListening)
    }
}
