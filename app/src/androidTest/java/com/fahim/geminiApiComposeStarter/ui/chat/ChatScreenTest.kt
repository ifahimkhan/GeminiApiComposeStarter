package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun placeholder_isDisplayed_whenMessagesEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Response will be displayed here!")
            .assertIsDisplayed()
    }

    @Test
    fun messages_areDisplayed_inChatBubbles() {
        val messages = listOf(
            ChatMessage(text = "Hello from User", participant = Participant.USER),
            ChatMessage(text = "Hello from Gemini", participant = Participant.MODEL),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello from User").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello from Gemini").assertIsDisplayed()
    }

    @Test
    fun promptInput_triggersOnPromptChange() {
        var enteredText = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = {},
                )
            }
        }

        composeTestRule
            .onNodeWithText("Enter your prompt here")
            .performTextInput("Test query")

        assertEquals("Test query", enteredText)
    }

    @Test
    fun sendButton_triggersOnSend() {
        var sendClicked = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Ready to send"),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Send")
            .performClick()

        assertTrue(sendClicked)
    }
}
