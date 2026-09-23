package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.MessageRole
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
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
    fun emptyState_displaysWelcomeMessage() {
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
            .onNodeWithText("Send a message to start chatting with Gemini!")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessages_displayedInBubbleList() {
        val testMessages = listOf(
            ChatMessage(id = 1, role = MessageRole.USER, content = "Hello Gemini!"),
            ChatMessage(id = 2, role = MessageRole.MODEL, content = "Hello! How can I assist you today?"),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = testMessages),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I assist you today?").assertIsDisplayed()
    }

    @Test
    fun promptBar_typingAndClickingSend() {
        var enteredText = ""
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = { sendClicked = true },
                )
            }
        }

        val inputField = composeTestRule.onNodeWithText("Ask Gemini anything…")
        inputField.performTextInput("Tell me a fun fact")
        assertEquals("Tell me a fun fact", enteredText)

        // Render updated state with prompt text
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = enteredText),
                    onPromptChange = { enteredText = it },
                    onSend = { sendClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun loadingState_showsThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(id = 1, role = MessageRole.USER, content = "Testing loading")
                        ),
                        isLoading = true,
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking…").assertIsDisplayed()
    }
}
