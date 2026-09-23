package com.devraj.geminiassistant

import com.devraj.geminiassistant.data.GeminiRepository
import com.devraj.geminiassistant.data.local.entity.ChatMessageEntity
import com.devraj.geminiassistant.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeGeminiRepository : GeminiRepository {
    private val messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    private val instructionFlow = MutableStateFlow("You are an intelligent, concise, and helpful AI assistant.")
    private val temperatureFlow = MutableStateFlow(0.7f)
    private val modelFlow = MutableStateFlow("gemini-3.6-flash")

    var shouldFail: Boolean = false

    override fun getMessagesFlow(): Flow<List<ChatMessageEntity>> = messagesFlow

    override fun getSystemInstructionFlow(): Flow<String> = instructionFlow

    override fun getTemperatureFlow(): Flow<Float> = temperatureFlow

    override fun getSelectedModelFlow(): Flow<String> = modelFlow

    override fun generateStream(prompt: String): Flow<String> = flow {
        val current = messagesFlow.value.toMutableList()
        current.add(ChatMessageEntity(id = current.size + 1L, text = prompt, isUser = true))

        if (shouldFail) {
            val errorMsg = "API quota exceeded"
            current.add(ChatMessageEntity(id = current.size + 1L, text = errorMsg, isUser = false, isError = true))
            messagesFlow.value = current
            throw RuntimeException(errorMsg)
        }

        emit("Echo: ")
        emit(prompt)

        val reply = "Echo: $prompt"
        current.add(ChatMessageEntity(id = current.size + 1L, text = reply, isUser = false))
        messagesFlow.value = current
    }

    override suspend fun generateText(prompt: String): Result<String> {
        val current = messagesFlow.value.toMutableList()
        current.add(ChatMessageEntity(id = current.size + 1L, text = prompt, isUser = true))

        if (shouldFail) {
            val errorMsg = "API quota exceeded"
            current.add(ChatMessageEntity(id = current.size + 1L, text = errorMsg, isUser = false, isError = true))
            messagesFlow.value = current
            return Result.failure(RuntimeException(errorMsg))
        }

        val reply = "Echo: $prompt"
        current.add(ChatMessageEntity(id = current.size + 1L, text = reply, isUser = false))
        messagesFlow.value = current
        return Result.success(reply)
    }

    override suspend fun clearHistory() {
        messagesFlow.value = emptyList()
    }

    override suspend fun updatePreferences(instruction: String, temperature: Float, model: String) {
        instructionFlow.value = instruction
        temperatureFlow.value = temperature
        modelFlow.value = model
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(fakeRepository, hasApiKey = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_state_has_empty_messages_and_default_settings() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertEquals("", state.prompt)
        assertEquals("You are an intelligent, concise, and helpful AI assistant.", state.systemInstruction)
        assertEquals(0.7f, state.temperature, 0.01f)
        assertEquals("gemini-3.6-flash", state.selectedModel)
    }

    @Test
    fun onPromptChange_updates_prompt_correctly() = runTest(testDispatcher) {
        viewModel.onPromptChange("Hello Gemini")
        advanceUntilIdle()
        assertEquals("Hello Gemini", viewModel.uiState.value.prompt)
    }

    @Test
    fun onVoiceResult_updates_prompt_from_speech_recognition() = runTest(testDispatcher) {
        viewModel.onVoiceResult("Voice query from Devraj")
        advanceUntilIdle()
        assertEquals("Voice query from Devraj", viewModel.uiState.value.prompt)
    }

    @Test
    fun onSend_successfully_adds_user_and_assistant_messages() = runTest(testDispatcher) {
        viewModel.onPromptChange("What is Jetpack Compose?")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertFalse(state.isLoading)
        assertEquals(2, state.messages.size)
        assertEquals("What is Jetpack Compose?", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Echo: What is Jetpack Compose?", state.messages[1].text)
        assertFalse(state.messages[1].isUser)
    }

    @Test
    fun onSend_handles_failure_and_sets_error_message() = runTest(testDispatcher) {
        fakeRepository.shouldFail = true
        viewModel.onPromptChange("Will fail")
        viewModel.onSend()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertEquals("API quota exceeded", state.errorMessage)
    }

    @Test
    fun clearChat_empties_conversation_history() = runTest(testDispatcher) {
        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.messages.size)

        viewModel.clearChat()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.messages.isEmpty())
    }

    @Test
    fun saveSettings_updates_instructions_temperature_and_model() = runTest(testDispatcher) {
        viewModel.saveSettings("Be concise and technical.", 0.3f, "gemini-1.5-pro")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Be concise and technical.", state.systemInstruction)
        assertEquals(0.3f, state.temperature, 0.01f)
        assertEquals("gemini-1.5-pro", state.selectedModel)
        assertFalse(state.isSettingsOpen)
    }

    @Test
    fun search_and_tts_state_updates_properly() = runTest(testDispatcher) {
        viewModel.toggleSearch(true)
        viewModel.onSearchQueryChange("Jetpack")
        viewModel.setSpeakingMessageId(42L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSearchOpen)
        assertEquals("Jetpack", state.searchQuery)
        assertEquals(42L, state.speakingMessageId)
    }
}
