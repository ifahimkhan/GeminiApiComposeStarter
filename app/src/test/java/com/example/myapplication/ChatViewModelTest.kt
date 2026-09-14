package com.example.myapplication

import com.example.myapplication.data.model.ChatRole
import com.example.myapplication.ui.chat.ChatViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
    private lateinit var fakePreferencesRepository: FakeUserPreferencesRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeGeminiRepository()
        fakePreferencesRepository = FakeUserPreferencesRepository()
        viewModel = ChatViewModel(fakeRepository, fakePreferencesRepository)
    }

    @Test
    fun initialState_isCorrect() = runTest {
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals("", state.inputText)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.autoScrollEnabled)
        assertNull(state.darkModeOverride)
    }

    @Test
    fun onInputTextChanged_updatesInputText() = runTest {
        viewModel.onInputTextChanged("Hello Gemini")
        assertEquals("Hello Gemini", viewModel.uiState.value.inputText)
    }

    @Test
    fun onSpeechRecognized_appendsToInputText() = runTest {
        viewModel.onSpeechRecognized("Hello")
        assertEquals("Hello", viewModel.uiState.value.inputText)

        viewModel.onSpeechRecognized("World")
        assertEquals("Hello World", viewModel.uiState.value.inputText)
    }

    @Test
    fun sendMessage_successfulResponse_updatesStateAndClearsInput() = runTest {
        viewModel.onInputTextChanged("Tell me a joke")
        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("", state.inputText)
        assertEquals(2, state.messages.size)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("Tell me a joke", state.messages[0].text)
        assertEquals(ChatRole.MODEL, state.messages[1].role)
        assertEquals("Echo: Tell me a joke", state.messages[1].text)
        assertNull(state.errorMessage)
    }

    @Test
    fun sendMessage_errorResponse_setsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.onInputTextChanged("Fail test")
        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to connect"))
    }

    @Test
    fun clearError_resetsErrorMessage() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.onInputTextChanged("Fail test")
        viewModel.sendMessage()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearHistory_clearsMessagesInRepositoryAndState() = runTest {
        viewModel.onInputTextChanged("Message 1")
        viewModel.sendMessage()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearHistory()
        assertEquals(0, viewModel.uiState.value.messages.size)
    }
}
