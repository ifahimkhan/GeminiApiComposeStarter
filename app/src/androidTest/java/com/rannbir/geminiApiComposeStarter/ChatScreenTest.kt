package com.rannbir.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.rannbir.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.rannbir.geminiApiComposeStarter.ui.chat.ChatScreen
import com.rannbir.geminiApiComposeStarter.ui.chat.ChatUiState
import com.rannbir.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displaysWelcomeAndStudentInfo() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        userDisplayName = "Rannbir Sachdeva",
                        rollNumber = "N087",
                    ),
                    onPromptChange = {},
                    onVoiceInputResult = {},
                    onSend = {},
                    onClearChat = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to Gemini Chat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Student: Rannbir Sachdeva • Roll No: N087").assertIsDisplayed()
    }

    @Test
    fun chatMessages_displayedAsBubbles() {
        val messages = listOf(
            ChatMessageEntity(id = 1, text = "User test message", isFromUser = true),
            ChatMessageEntity(id = 2, text = "Gemini response text", isFromUser = false),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onVoiceInputResult = {},
                    onSend = {},
                    onClearChat = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("User test message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini response text").assertIsDisplayed()
    }

    @Test
    fun sendButtonAndVoiceButton_areDisplayed() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onVoiceInputResult = {},
                    onSend = {},
                    onClearChat = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice Input").assertIsDisplayed()
    }
}
