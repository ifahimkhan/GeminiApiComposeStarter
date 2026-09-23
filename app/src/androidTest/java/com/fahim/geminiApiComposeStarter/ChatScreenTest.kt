package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.model.ChatMessage
import com.fahim.geminiApiComposeStarter.data.model.Participant
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysWelcomeHeader() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList()),
                    onPromptChange = {},
                    onSend = {},
                    onStartVoiceInput = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini Pulse").assertIsDisplayed()
        composeTestRule.onNodeWithText("How can I assist you today? Type a prompt below or tap the microphone to start.").assertIsDisplayed()
    }

    @Test
    fun messages_renderBothUserAndGeminiBubbles() {
        val testMessages = listOf(
            ChatMessage(text = "Hello Gemini from Compose test", participant = Participant.USER),
            ChatMessage(text = "Hello human, I am ready to help!", participant = Participant.GEMINI),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {},
                    onStartVoiceInput = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini from Compose test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello human, I am ready to help!").assertIsDisplayed()
    }

    @Test
    fun promptBar_typingAndSendingTriggersCallbacks() {
        var enteredText = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = { sendClicked = true },
                    onStartVoiceInput = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Message Gemini Pulse…").performTextInput("How does Jetpack Compose work?")
        assertEquals("How does Jetpack Compose work?", enteredText)
    }

    @Test
    fun loadingState_showsThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                    onStartVoiceInput = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini Pulse is responding…").assertIsDisplayed()
    }
}
