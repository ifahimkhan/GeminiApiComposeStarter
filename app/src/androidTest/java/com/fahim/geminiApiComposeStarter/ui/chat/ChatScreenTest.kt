package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.preferences.ThemePreference
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userAndGeminiMessages_areDisplayed() {

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                id = 1L,
                                text = "Hello Gemini",
                                sender = MessageSender.USER
                            ),
                            ChatMessage(
                                id = 2L,
                                text = "Hello from Gemini",
                                sender = MessageSender.GEMINI
                            )
                        )
                    ),
                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                    themePreference =
                        ThemePreference.SYSTEM,
                    onThemePreferenceChange = {},
                    onPromptChange = {},
                    onSend = {},
                    onVoiceClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Hello Gemini")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Hello from Gemini")
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsProgressIndicator() {

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                id = 1L,
                                text = "Hello",
                                sender = MessageSender.USER
                            )
                        ),
                        isLoading = true
                    ),
                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                    themePreference =
                        ThemePreference.SYSTEM,
                    onThemePreferenceChange = {},
                    onPromptChange = {},
                    onSend = {},
                    onVoiceClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "ProgressBar"
            )
    }

    @Test
    fun promptField_acceptsText() {

        var enteredText = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        prompt = enteredText
                    ),
                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                    themePreference =
                        ThemePreference.SYSTEM,
                    onThemePreferenceChange = {},
                    onPromptChange = {
                        enteredText = it
                    },
                    onSend = {},
                    onVoiceClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Enter your prompt here"
            )
            .performTextInput(
                "Test message"
            )
    }

    @Test
    fun sendButton_isDisplayed() {

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                    themePreference =
                        ThemePreference.SYSTEM,
                    onThemePreferenceChange = {},
                    onPromptChange = {},
                    onSend = {},
                    onVoiceClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Send"
            )
            .assertIsDisplayed()
    }

    @Test
    fun voiceButton_isDisplayed() {

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                    themePreference =
                        ThemePreference.SYSTEM,
                    onThemePreferenceChange = {},
                    onPromptChange = {},
                    onSend = {},
                    onVoiceClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Voice input"
            )
            .assertIsDisplayed()
    }
}