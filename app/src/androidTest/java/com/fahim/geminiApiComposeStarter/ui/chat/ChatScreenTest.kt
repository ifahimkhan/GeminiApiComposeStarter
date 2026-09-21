package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_initialState_displaysEmptyGreetingAndPromptBar() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        // Screen is displayed
        composeTestRule.onNodeWithTag("chat_screen").assertIsDisplayed()

        // Empty greeting is shown
        composeTestRule.onNodeWithText("How can I help you today?").assertIsDisplayed()

        // Input field is displayed
        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()

        // Voice button is displayed
        composeTestRule.onNodeWithTag("voice_input_button").assertIsDisplayed().assertHasClickAction()

        // Send button is disabled for empty prompt
        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }

    @Test
    fun chatScreen_inputPrompt_enablesSendButtonAndTriggersCallback() {
        var sendClicked = false

        composeTestRule.setContent {
            var prompt by remember { mutableStateOf("") }
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = prompt),
                    onPromptChange = { prompt = it },
                    onSend = { sendClicked = true },
                )
            }
        }

        // Type input into prompt
        composeTestRule.onNodeWithTag("prompt_input").performTextInput("Hello Gemini")

        // Send button should now be enabled and clickable
        composeTestRule.onNodeWithTag("send_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("send_button").performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun chatScreen_displaysUserAndGeminiMessages() {
        val messages = listOf(
            ChatMessage(id = "1", text = "What is Kotlin?", participant = Participant.USER),
            ChatMessage(id = "2", text = "Kotlin is a modern language by JetBrains.", participant = Participant.GEMINI)
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

        // Verify messages list is displayed
        composeTestRule.onNodeWithTag("messages_list").assertIsDisplayed()

        // Verify both messages are rendered
        composeTestRule.onNodeWithText("What is Kotlin?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin is a modern language by JetBrains.").assertIsDisplayed()
    }

    @Test
    fun chatScreen_loadingState_showsLoadingBubbleAndDisablesControls() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(ChatMessage(id = "1", text = "Tell me a joke", participant = Participant.USER)),
                        isLoading = true
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        // Loading indicator is visible
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Gemini is thinking...").onFirst().assertIsDisplayed()

        // Prompt input and send are disabled during loading
        composeTestRule.onNodeWithTag("prompt_input").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }
}
