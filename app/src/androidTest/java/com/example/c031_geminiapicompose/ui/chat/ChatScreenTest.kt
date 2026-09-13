package com.example.c031_geminiapicompose.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.c031_geminiapicompose.data.ChatMessage
import com.example.c031_geminiapicompose.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysPlaceholder_whenMessagesEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Response will be displayed here!").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysMessages_whenPresent() {
        val messages = listOf(
            ChatMessage(id = 1, text = "Hello Gemini!", isUser = true),
            ChatMessage(id = 2, text = "Hello Human!", isUser = false)
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello Human!").assertIsDisplayed()
    }

    @Test
    fun chatScreen_inputAndSend_triggersCallbacks() {
        var inputCaptured = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = inputCaptured),
                    onPromptChange = { inputCaptured = it },
                    onSend = { sendClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Test Prompt")
        composeTestRule.onNodeWithContentDescription("Send").performClick()

        assert(sendClicked)
    }

    @Test
    fun chatScreen_showsLoadingIndicator_whenIsLoadingTrue() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }
}
