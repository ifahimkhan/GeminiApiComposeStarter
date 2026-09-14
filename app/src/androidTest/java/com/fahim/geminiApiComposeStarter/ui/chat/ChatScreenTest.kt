package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun typingAndTappingSend_forwardsTheTypedPromptAndTriggersSend() {
        var lastPromptValue = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = ""),
                    onPromptChange = { lastPromptValue = it },
                    onSend = { sendClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("prompt_field").performTextInput("Hello Gemini")
        composeTestRule.onNodeWithTag("send_button").performClick()

        assertEquals("Hello Gemini", lastPromptValue)
        assertTrue(sendClicked)
    }

    @Test
    fun loadingState_showsTheProgressIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun errorState_showsTheErrorMessage() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(errorMessage = "Something went wrong"),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun existingMessages_areDisplayedInTheList() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(id = "1", text = "Hi", isFromUser = true),
                            ChatMessage(id = "2", text = "Hello there", isFromUser = false),
                        ),
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello there").assertIsDisplayed()
    }
}