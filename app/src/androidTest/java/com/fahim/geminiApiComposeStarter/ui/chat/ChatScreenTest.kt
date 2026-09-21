package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun topAppBar_rendersTitle() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini Chat").assertIsDisplayed()
    }

    @Test
    fun promptField_isDisplayed() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your prompt here").assertIsDisplayed()
    }

    @Test
    fun sendButton_isDisplayed() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed()
    }

    @Test
    fun voiceInputButton_isDisplayed() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }

    @Test
    fun promptField_typing_invokesOnPromptChange() {
        var capturedPrompt = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = ""),
                    onPromptChange = { capturedPrompt = it },
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Enter your prompt here").performTextInput("Hello Gemini")
        assertEquals("Hello Gemini", capturedPrompt)
    }

    @Test
    fun sendButton_click_invokesOnSend() {
        var onSendCalled = false
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Hello Gemini"),
                    onPromptChange = {},
                    onSend = { onSendCalled = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Send").performClick()
        assertTrue(onSendCalled)
    }

    @Test
    fun chatMessages_renderUserAndModelBubbles() {
        val messages = listOf(
            ChatMessage(
                id = "1",
                text = "Hello from User",
                participant = Participant.USER,
            ),
            ChatMessage(
                id = "2",
                text = "Hello from Gemini",
                participant = Participant.MODEL,
            ),
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
    fun loadingState_displaysThinkingIndicator() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Thinking...").assertIsDisplayed()
    }

    @Test
    fun themeToggle_reflectsThemeState() {
        var toggledMode: Boolean? = null
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isDarkMode = false),
                    onPromptChange = {},
                    onSend = {},
                    onSetDarkMode = { toggledMode = it },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Switch to dark theme").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Switch to dark theme").performClick()
        assertEquals(true, toggledMode)

        // Also verify the dark-state semantics by rendering with isDarkMode = true
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isDarkMode = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Switch to light theme").assertIsDisplayed()
    }
}
