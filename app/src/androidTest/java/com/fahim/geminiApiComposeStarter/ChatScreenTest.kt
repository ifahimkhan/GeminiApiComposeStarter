package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {

    @get:Rule
    val composeTestRule =
        createComposeRule()

    @Test
    fun emptyChat_showsWelcomeMessage() {

        composeTestRule.setContent {

            GeminiApiComposeStarterTheme {

                ChatScreen(
                    state = ChatUiState(),

                    onPromptChange = {},

                    onSend = {},

                    onErrorShown = {},

                    onConciseRepliesChange = {},

                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Start a conversation with Gemini"
            )
            .assertIsDisplayed()
    }

    @Test
    fun chatScreen_showsUserAndGeminiMessages() {

        composeTestRule.setContent {

            GeminiApiComposeStarterTheme {

                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(

                            ChatMessage(
                                id = 1L,
                                text = "Hello Gemini",
                                sender =
                                    MessageSender.USER,
                            ),

                            ChatMessage(
                                id = 2L,
                                text = "Hello!",
                                sender =
                                    MessageSender.GEMINI,
                            ),
                        ),
                    ),

                    onPromptChange = {},

                    onSend = {},

                    onErrorShown = {},

                    onConciseRepliesChange = {},

                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Hello Gemini"
            )
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(
                "Hello!"
            )
            .assertIsDisplayed()
    }

    @Test
    fun loadingState_showsThinkingMessage() {

        composeTestRule.setContent {

            GeminiApiComposeStarterTheme {

                ChatScreen(
                    state = ChatUiState(
                        isLoading = true
                    ),

                    onPromptChange = {},

                    onSend = {},

                    onErrorShown = {},

                    onConciseRepliesChange = {},

                    windowWidthSizeClass =
                        WindowWidthSizeClass.Compact,
                )
            }
        }

        composeTestRule
            .onNodeWithText(
                "Gemini is thinking..."
            )
            .assertIsDisplayed()
    }
}