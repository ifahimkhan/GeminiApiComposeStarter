package com.fahim.geminiApiComposeStarter

import android.content.Context
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.chat.Participant
import com.fahim.geminiApiComposeStarter.ui.chat.SettingsUiState
import com.fahim.geminiApiComposeStarter.ui.settings.SettingsScreen
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun emptyChatView_isDisplayed_whenMessagesEmpty() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(messages = emptyList(), isLoading = false),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        // Welcome headline and prompt placeholder
        composeTestRule.onNodeWithText(context.getString(R.string.hello_from_gemini)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.how_can_i_help)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.suggestion_explain)).assertIsDisplayed()
    }

    @Test
    fun voiceInputButton_hasCorrectContentDescription() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.voice_input))
            .assertIsDisplayed()
    }

    @Test
    fun sendButton_isDisabledWhenPromptIsEmpty_andEnabledWhenPromptHasText() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = ""),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.send))
            .assertIsNotEnabled()

        // Recompose with text in prompt
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(prompt = "Hello Gemini"),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.send))
            .assertIsEnabled()
    }

    @Test
    fun textInput_triggersOnPromptChange() {
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

        composeTestRule.onNodeWithText(context.getString(R.string.ask_gemini_anything))
            .performTextInput("Testing Compose UI")

        assertEquals("Testing Compose UI", capturedPrompt)
    }

    @Test
    fun chatMessages_areDisplayed_whenPresentInState() {
        val testMessages = listOf(
            ChatMessage(
                text = "Explain encryption",
                participant = Participant.USER,
                timestamp = "10:00 AM",
            ),
            ChatMessage(
                text = "Encryption transforms readable plaintext into unreadable ciphertext.",
                participant = Participant.GEMINI,
                timestamp = "10:01 AM",
            ),
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

        composeTestRule.onNodeWithText("Explain encryption").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Encryption transforms readable plaintext into unreadable ciphertext.",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun thinkingIndicator_isDisplayed_whenLoading() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(text = "Hello", participant = Participant.USER),
                        ),
                        isLoading = true,
                    ),
                    onPromptChange = {},
                    onSend = {},
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.gemini_thinking)).assertIsDisplayed()
    }

    @Test
    fun errorCard_isDisplayed_andRetryDismissButtonsWork() {
        var dismissClicked = false
        var retryClicked = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        errorMessage = "Rate limit reached. Please wait a moment.",
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onDismissError = { dismissClicked = true },
                    onRetry = { retryClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Rate limit reached. Please wait a moment.").assertIsDisplayed()

        composeTestRule.onNodeWithText(context.getString(R.string.dismiss)).performClick()
        assertTrue(dismissClicked)

        composeTestRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun settingsScreen_rendersAllSections() {
        var backClicked = false
        var themeToggled = false

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                SettingsScreen(
                    settings = SettingsUiState(isDarkTheme = false, saveHistory = true),
                    onBack = { backClicked = true },
                    onThemeToggle = { themeToggled = true },
                    onSaveHistoryToggle = {},
                    onClearHistory = {},
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.appearance)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.chat_history)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.input_and_voice)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.about)).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assertTrue(backClicked)
    }
}
