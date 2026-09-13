package com.example.c020_harsh_assignment1.ui.chat

import com.example.c020_harsh_assignment1.data.GeminiRepository
import com.example.c020_harsh_assignment1.data.PreferencesRepository
import com.example.c020_harsh_assignment1.data.local.ChatDao
import com.example.c020_harsh_assignment1.data.local.ChatMessage
import com.example.c020_harsh_assignment1.security.CryptoManager
import com.google.ai.client.generativeai.type.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeChatDao: FakeChatDao
    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var fakeCryptoManager: FakeCryptoManager
    private lateinit var fakeGeminiRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeChatDao = FakeChatDao()
        fakePreferencesRepository = FakePreferencesRepository()
        fakeCryptoManager = FakeCryptoManager()
        fakeGeminiRepository = FakeGeminiRepository()
        
        viewModel = ChatViewModel(
            chatDao = fakeChatDao,
            preferencesRepository = fakePreferencesRepository,
            cryptoManager = fakeCryptoManager,
            buildConfigApiKey = "test_api_key",
            repositoryFactory = { fakeGeminiRepository }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onPromptChange updates prompt in uiState`() = runTest {
        viewModel.onPromptChange("Hello")
        assertEquals("Hello", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onSend with empty prompt sets EMPTY error`() = runTest {
        viewModel.onPromptChange("")
        viewModel.onSend()
        assertEquals(PromptError.EMPTY, viewModel.uiState.value.promptError)
    }

    @Test
    fun `onStyleChange updates preference and uiState`() = runTest {
        viewModel.onStyleChange("Detailed")
        advanceUntilIdle()
        assertEquals("Detailed", viewModel.uiState.value.responseStyle)
    }

    @Test
    fun `successful gemini response updates state and saves to dao`() = runTest {
        advanceUntilIdle() // Init security
        
        val userPrompt = "How are you?"
        val geminiResponse = "I am a fake AI"
        fakeGeminiRepository.nextResult = Result.success(geminiResponse)
        
        viewModel.onPromptChange(userPrompt)
        viewModel.onSend()
        
        // Before response
        assertTrue(viewModel.uiState.value.isLoading)
        
        advanceUntilIdle()
        
        // After response
        assertFalse(viewModel.uiState.value.isLoading)
        val messages = fakeChatDao.getAllMessages().first()
        assertEquals(2, messages.size)
        assertEquals(userPrompt, messages[0].text)
        assertEquals(geminiResponse, messages[1].text)
    }

    @Test
    fun `failed gemini response updates state with error`() = runTest {
        advanceUntilIdle() // Init security
        
        val errorMessage = "API Key Invalid"
        fakeGeminiRepository.nextResult = Result.failure(Exception(errorMessage))
        
        viewModel.onPromptChange("Hi")
        viewModel.onSend()
        
        advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(errorMessage, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `sending while loading does not trigger second request`() = runTest {
        advanceUntilIdle()
        
        viewModel.onPromptChange("Message 1")
        viewModel.onSend()
        
        // isLoading should be true immediately
        assertTrue(viewModel.uiState.value.isLoading)
        
        // Execute the coroutine until it hits the suspend point (generateText)
        testDispatcher.scheduler.runCurrent()
        
        assertEquals(1, fakeGeminiRepository.callCount)
        
        viewModel.onPromptChange("Message 2")
        viewModel.onSend()
        
        assertEquals(1, fakeGeminiRepository.callCount)
    }

    private class FakeChatDao : ChatDao {
        private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
        private var idGenerator: Long = 1
        override fun getAllMessages(): Flow<List<ChatMessage>> = _messages
        override suspend fun insertMessage(message: ChatMessage): Long {
            val newMessage = message.copy(id = idGenerator++)
            _messages.value = _messages.value + newMessage
            return newMessage.id
        }
        override suspend fun clearHistory(): Int {
            val count = _messages.value.size
            _messages.value = emptyList()
            return count
        }
    }

    private class FakePreferencesRepository : PreferencesRepository {
        private val _encryptedKey = MutableStateFlow<String?>(null)
        private val _style = MutableStateFlow("Simple")
        override val encryptedApiKey: Flow<String?> = _encryptedKey
        override val responseStyle: Flow<String> = _style
        override suspend fun saveEncryptedApiKey(key: String) { _encryptedKey.value = key }
        override suspend fun saveResponseStyle(style: String) { _style.value = style }
    }

    private class FakeCryptoManager : CryptoManager {
        override fun encrypt(bytes: ByteArray): ByteArray = bytes
        override fun decrypt(bytes: ByteArray): ByteArray = bytes
        override fun encodeToString(bytes: ByteArray): String = String(bytes)
        override fun decodeFromString(text: String): ByteArray = text.toByteArray()
    }

    private class FakeGeminiRepository : GeminiRepository {
        var nextResult: Result<String> = Result.success("Default response")
        var callCount = 0
        override suspend fun generateText(prompt: String, history: List<Content>): Result<String> {
            callCount++
            return nextResult
        }
    }
}
