package com.example.geminiapicomposestarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.geminiapicomposestarter.data.ChatMessage
import com.example.geminiapicomposestarter.data.Participant
import com.example.geminiapicomposestarter.ui.chat.ChatScreen
import com.example.geminiapicomposestarter.ui.chat.ChatViewModel
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyChat_showsPlaceholder() {
        val viewModel = ChatViewModel()
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("emptyChatBox").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start a conversation with Gemini!", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun typingInPromptField_updatesText() {
        val viewModel = ChatViewModel()
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("promptInputField")
            .performTextInput("Hello, who is the founder of Google")

        composeTestRule.onNodeWithText("Hello, who is the founder of Google")
            .assertIsDisplayed()
    }

    @Test
    fun whenIdle_sendButtonIsEnabled() {
        val viewModel = ChatViewModel()
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("promptInputField")
            .performTextInput("Valid prompt")

        composeTestRule.onNodeWithTag("sendButton")
            .assertIsEnabled()
    }

    @Test
    fun whileLoading_sendButtonIsDisabled() {
        val viewModel = ChatViewModel()
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("promptInputField")
            .performTextInput("Test loading")

        composeTestRule.onNodeWithTag("sendButton").performClick()

        // When sending, the prompt field clears and button is disabled
        composeTestRule.onNodeWithTag("sendButton")
            .assertIsNotEnabled()
    }

    @Test
    fun emptyPromptError_isShown() {
        val viewModel = ChatViewModel()
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        viewModel.sendMessage("   ")

        composeTestRule.onNodeWithText("Prompt cannot be empty.")
            .assertIsDisplayed()
    }

    @Test
    fun sendButton_invokesOnSend() {
        val viewModel = ChatViewModel()
        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithTag("promptInputField")
            .performTextInput("Hello Gemini")

        composeTestRule.onNodeWithTag("sendButton")
            .performClick()

        composeTestRule.onNodeWithText("Hello Gemini")
            .assertIsDisplayed()
    }

    @Test
    fun messages_areDisplayed() {
        val viewModel = ChatViewModel()
        val userMsg = ChatMessage(text = "Hello, who is the founder of Google", participant = Participant.USER)
        viewModel.sendMessage("Hello, who is the founder of Google")

        composeTestRule.setContent {
            ChatScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Hello, who is the founder of Google")
            .assertIsDisplayed()
    }
}
