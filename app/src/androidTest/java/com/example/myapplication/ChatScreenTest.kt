package com.example.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.myapplication.ui.chat.ChatScreen
import com.example.myapplication.ui.chat.ChatViewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
    fun chatInput_initialState_isDisplayedAndSendButtonDisabled() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ChatScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("chat_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }

    @Test
    fun chatInput_typingText_enablesSendButton() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ChatScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("chat_input_field").performTextInput("Hello Gemini")
        composeTestRule.onNodeWithTag("send_button").assertIsEnabled()
    }

    @Test
    fun sendMessage_displaysUserAndModelMessages() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ChatScreen(viewModel = viewModel)
            }
        }

        composeTestRule.onNodeWithTag("chat_input_field").performTextInput("Test query")
        composeTestRule.onNodeWithTag("send_button").performClick()

        composeTestRule.onNodeWithText("Test query").assertIsDisplayed()
        composeTestRule.onNodeWithText("Echo: Test query").assertIsDisplayed()
    }
}
