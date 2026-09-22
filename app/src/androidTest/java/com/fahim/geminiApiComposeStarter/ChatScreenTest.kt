package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ChatScreen using createComposeRule().
 */
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatScreen_displaysHeaderAndEmptyStatePlaceholder() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onClearHistory = {},
                    onVoiceInputResult = {},
                    onDismissError = {},
                )
            }
        }

        // Verify TopAppBar Title and Roll Number Badge
        composeTestRule.onNodeWithText("Gemini AI Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Roll: N142").assertIsDisplayed()

        // Verify Empty State Placeholder
        composeTestRule.onNodeWithText("Start a Conversation").assertIsDisplayed()

        // Verify Voice and Send icons exist
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysMessagesInLazyColumn() {
        val messages = listOf(
            ChatMessage(id = 1, text = "User test message", isFromUser = true),
            ChatMessage(id = 2, text = "Gemini test response", isFromUser = false)
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = messages),
                    onPromptChange = {},
                    onSend = {},
                    onClearHistory = {},
                    onVoiceInputResult = {},
                    onDismissError = {},
                )
            }
        }

        // Verify user and bot messages are displayed in bubbles
        composeTestRule.onNodeWithText("User test message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gemini test response").assertIsDisplayed()

        // Verify clear chat action is displayed when messages exist
        composeTestRule.onNodeWithContentDescription("Clear chat history").assertIsDisplayed()
    }

    @Test
    fun chatScreen_enteringText_triggersPromptChange() {
        var enteredText = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = {},
                    onClearHistory = {},
                    onVoiceInputResult = {},
                    onDismissError = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Ask Gemini anything…").performTextInput("Hello Gemini")
        assert(enteredText == "Hello Gemini")
    }
}
