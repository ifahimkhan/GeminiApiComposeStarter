package com.fahim.geminiApiComposeStarter.ui.chat

import com.fahim.geminiApiComposeStarter.MainDispatcherRule
import com.fahim.geminiApiComposeStarter.data.ChatMessageEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var fakeDao: FakeChatMessageDao
    private lateinit var viewModel: ChatViewModel

    private fun createViewModel(hasApiKey: Boolean = true) {
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = hasApiKey, dao = fakeDao)
    }

    @Test
    fun `sending a valid prompt appends user and gemini messages`() = runTest {
        createViewModel()
        fakeRepository.result = Result.success("Hello from Gemini")

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals(2, state.messages.size)
        assertEquals("Hi there", state.messages[0].text)
        assertTrue(state.messages[0].isFromUser)
        assertEquals("Hello from Gemini", state.messages[1].text)
        assertTrue(!state.messages[1].isFromUser)
        assertEquals(false, state.isLoading)
        assertEquals("", state.prompt)
    }

    @Test
    fun `sending an empty prompt sets a prompt error and does not call the repository`() = runTest {
        createViewModel()

        viewModel.onPromptChange("   ")
        viewModel.onSend()

        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
        assertNull(fakeRepository.lastPrompt)
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun `sending with no api key sets an error message and does not call the repository`() = runTest {
        createViewModel(hasApiKey = false)

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        assertEquals(ChatViewModel.MISSING_API_KEY_MESSAGE, viewModel.uiState.value.errorMessage)
        assertNull(fakeRepository.lastPrompt)
    }

    @Test
    fun `a failed repository call sets an error message and stops loading`() = runTest {
        createViewModel()
        fakeRepository.result = Result.failure(RuntimeException("Network error"))

        viewModel.onPromptChange("Hi there")
        viewModel.onSend()

        val state = viewModel.uiState.value
        assertEquals("Network error", state.errorMessage)
        assertEquals(false, state.isLoading)
        assertEquals(1, state.messages.size) // the user message was still saved before the call failed
    }

    @Test
    fun `messages already in the database are loaded into ui state on start`() = runTest {
        fakeRepository = FakeGeminiRepository()
        fakeDao = FakeChatMessageDao()
        fakeDao.insert(ChatMessageEntity(id = "1", text = "Existing message", isFromUser = true))

        viewModel = ChatViewModel(repository = fakeRepository, hasApiKey = true, dao = fakeDao)

        val state = viewModel.uiState.value
        assertEquals(1, state.messages.size)
        assertEquals("Existing message", state.messages[0].text)
    }
}