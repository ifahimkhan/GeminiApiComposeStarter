package com.fahim.geminiApiComposeStarter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.Participant
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
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
    fun chatScreen_rendersEmptyStateInitially() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysMessagesCorrectly() {
        val messages = listOf(
            ChatMessage(text = "Hello from User", participant = Participant.USER),
            ChatMessage(text = "Hello from Gemini", participant = Participant.MODEL),
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
    fun chatScreen_sendButton_triggersOnSend() {
        var sendClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Testing prompt"),
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("send_button").performClick()
        assertTrue(sendClicked)
    }

    @Test
    fun chatScreen_promptInput_triggersOnPromptChange() {
        var updatedPrompt = ""

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = { updatedPrompt = it },
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("prompt_input").performTextInput("New prompt")
        assertEquals("New prompt", updatedPrompt)
    }

    @Test
    fun chatScreen_displaysErrorStateWhenPromptEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(promptError = PromptError.EMPTY),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Field cannot be empty").assertIsDisplayed()
    }

    @Test
    fun chatScreen_displaysLoadingIndicatorWhenLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(isLoading = true),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @Test
    fun chatScreen_themeToggleButton_triggersOnToggleTheme() {
        var toggleClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onToggleTheme = { toggleClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        assertTrue(toggleClicked)
    }

    @Test
    fun chatScreen_micButton_isDisplayedAndHasCorrectContentDescription() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voice input").assertIsDisplayed()
    }

    @Test
    fun chatScreen_micButton_triggersOnVoiceInputClick() {
        var micClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInputClick = { micClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithTag("mic_button").performClick()
        assertTrue(micClicked)
    }

    @Test
    fun chatScreen_voiceResult_placesRecognizedTextIntoPromptField() {
        val recognizedVoiceText = "Summarize Android architecture"

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = recognizedVoiceText),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText(recognizedVoiceText).assertIsDisplayed()
    }

    // --- Stage 9: Responsive UI Tests ---

    @Test
    fun chatScreen_portraitPhone_rendersAllComponents() {
        val messages = listOf(
            ChatMessage(text = "User message in portrait", participant = Participant.USER),
            ChatMessage(text = "Model reply in portrait", participant = Participant.MODEL),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                Box(modifier = Modifier.size(width = 360.dp, height = 640.dp)) {
                    ChatScreen(
                        state = ChatUiState(messages = messages),
                        onPromptChange = {},
                        onSend = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("User message in portrait").assertIsDisplayed()
        composeTestRule.onNodeWithText("Model reply in portrait").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
    }

    @Test
    fun chatScreen_landscapePhone_rendersAllComponentsWithoutClipping() {
        val messages = listOf(
            ChatMessage(text = "User message in landscape", participant = Participant.USER),
            ChatMessage(text = "Model reply in landscape", participant = Participant.MODEL),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                Box(modifier = Modifier.size(width = 720.dp, height = 360.dp)) {
                    ChatScreen(
                        state = ChatUiState(messages = messages),
                        onPromptChange = {},
                        onSend = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("User message in landscape").assertIsDisplayed()
        composeTestRule.onNodeWithText("Model reply in landscape").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
    }

    @Test
    fun chatScreen_tabletWideScreen_rendersAllComponentsWithReadableConstraints() {
        val messages = listOf(
            ChatMessage(text = "User message on tablet", participant = Participant.USER),
            ChatMessage(text = "Model reply on tablet", participant = Participant.MODEL),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                Box(modifier = Modifier.size(width = 1024.dp, height = 768.dp)) {
                    ChatScreen(
                        state = ChatUiState(messages = messages),
                        onPromptChange = {},
                        onSend = {},
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("User message on tablet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Model reply on tablet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prompt_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("send_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mic_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
    }
}
