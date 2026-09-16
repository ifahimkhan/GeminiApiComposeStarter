package com.example.c002_dhruvil_assignment1

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.c002_dhruvil_assignment1.ui.chat.ChatUiState
import com.example.c002_dhruvil_assignment1.ui.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeDao: FakeChatDao
    private lateinit var fakeRepository: FakeGeminiRepository
    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        fakeDao = FakeChatDao()
        fakeRepository = FakeGeminiRepository()
        viewModel = ChatViewModel(
            application = application,
            repository = fakeRepository,
            chatDao = fakeDao,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiStateIsInitial() = runTest {
        assertEquals(ChatUiState.Initial, viewModel.uiState.value)
    }

    @Test
    fun testSendMessageSuccessUpdatesUiStateAndPersistsMessages() = runTest {
        viewModel.sendMessage("Hello Gemini")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ChatUiState.Success)
        assertEquals("Fake Gemini Response", (viewModel.uiState.value as ChatUiState.Success).responseText)
        
        val messages = viewModel.messages.value
        assertEquals(2, messages.size)
        assertEquals("user", messages[0].sender)
        assertEquals("Hello Gemini", messages[0].text)
        assertEquals("gemini", messages[1].sender)
        assertEquals("Fake Gemini Response", messages[1].text)
    }

    @Test
    fun testSendMessageErrorUpdatesUiStateToError() = runTest {
        fakeRepository.shouldReturnError = true
        viewModel.sendMessage("Hello Gemini")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ChatUiState.Error)
        assertEquals("API Error", (viewModel.uiState.value as ChatUiState.Error).errorMessage)
    }

    @Test
    fun testClearHistoryClearsMessages() = runTest {
        viewModel.sendMessage("Hello Gemini")
        advanceUntilIdle()
        assertEquals(2, viewModel.messages.value.size)

        viewModel.clearHistory()
        advanceUntilIdle()

        assertEquals(0, viewModel.messages.value.size)
        assertEquals(ChatUiState.Initial, viewModel.uiState.value)
    }
}
