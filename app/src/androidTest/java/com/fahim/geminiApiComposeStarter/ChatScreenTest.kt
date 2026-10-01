package com.fahim.geminiApiComposeStarter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.model.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
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
    fun emptyChat_displaysPlaceholderAndInputControls() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onModelSelected = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        // Verify welcome text is shown
        composeTestRule
            .onNodeWithText("Hello! Ask me anything or tap the microphone to speak.")
            .assertIsDisplayed()

        // Verify prompt input, voice button, and send button exist
        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("voice_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()

        // Empty prompt -> send button is disabled
        composeTestRule.onNodeWithTag("send_button").assertIsNotEnabled()
    }

    @Test
    fun chatWithMessages_displaysUserAndGeminiBubbles() {
        val messages = listOf(
            ChatMessage(id = 1, text = "What is Kotlin?", isUser = true),
            ChatMessage(id = 2, text = "Kotlin is a modern programming language.", isUser = false),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onModelSelected = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        // Verify messages list is displayed
        composeTestRule.onNodeWithTag("messages_list").assertIsDisplayed()

        // Verify user bubble and text
        composeTestRule.onNodeWithTag("user_bubble").assertIsDisplayed()
        composeTestRule.onNodeWithText("What is Kotlin?").assertIsDisplayed()

        // Verify Gemini bubble and text
        composeTestRule.onNodeWithTag("gemini_bubble").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin is a modern programming language.").assertIsDisplayed()
    }

    @Test
    fun loadingState_showsThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(ChatMessage(id = 1, text = "Prompt", isUser = true)),
                        isLoading = true,
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceResult = {},
                    onModelSelected = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        // Verify generating / thinking indicator is displayed
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @Test
    fun typingPrompt_enablesSendButton_andInvokesOnSend() {
        var currentPrompt by mutableStateOf("")
        var sendInvoked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = currentPrompt),
                    onPromptChange = { currentPrompt = it },
                    onSend = { sendInvoked = true },
                    onVoiceResult = {},
                    onModelSelected = {},
                    onClearHistory = {},
                    onDismissError = {},
                )
            }
        }

        // Type input text
        composeTestRule.onNodeWithTag("prompt_input").performTextInput("Explain Jetpack Compose")

        composeTestRule.runOnIdle {
            assertEquals("Explain Jetpack Compose", currentPrompt)
        }

        // Send button should now be enabled
        composeTestRule.onNodeWithTag("send_button").assertIsEnabled()

        // Click send button
        composeTestRule.onNodeWithTag("send_button").performClick()
        assertTrue(sendInvoked)
    }
}
