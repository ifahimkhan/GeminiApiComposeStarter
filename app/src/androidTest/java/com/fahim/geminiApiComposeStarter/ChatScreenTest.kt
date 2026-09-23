package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.ThemePreference
import com.fahim.geminiApiComposeStarter.ui.chat.ChatMessage
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
    val composeRule = createComposeRule()

    private fun showScreen(
        state: ChatUiState = ChatUiState(),
        onSend: () -> Unit = {},
        onVoiceInput: () -> Unit = {},
        onThemeChange: (ThemePreference) -> Unit = {},
    ) {
        composeRule.setContent {
            GeminiApiComposeStarterTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                ChatScreen(
                    state = state,
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = {},
                    onSend = onSend,
                    onErrorShown = {},
                    onThemeChange = onThemeChange,
                    onVoiceInput = onVoiceInput
                )
            }
        }
    }

    @Test
    fun emptyChatShowsWelcomeAndDisablesSend() {
        showScreen()

        composeRule
            .onNodeWithText("Ask a question to start chatting.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Send message")
            .assertIsNotEnabled()
    }

    @Test
    fun typingEnablesSendAndPassesTypedText() {
        val state = mutableStateOf(ChatUiState())
        var sentText: String? = null

        composeRule.setContent {
            GeminiApiComposeStarterTheme(
                darkTheme = false,
                dynamicColor = false
            ) {
                ChatScreen(
                    state = state.value,
                    windowWidthSizeClass = WindowWidthSizeClass.Compact,
                    onPromptChange = { text ->
                        state.value = state.value.copy(prompt = text)
                    },
                    onSend = {
                        sentText = state.value.prompt
                    },
                    onErrorShown = {},
                    onThemeChange = {},
                    onVoiceInput = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("message_input")
            .performTextInput("Explain Kotlin")

        composeRule
            .onNodeWithContentDescription("Send message")
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals("Explain Kotlin", sentText)
        }
    }

    @Test
    fun conversationDisplaysUserAndAssistantMessages() {
        showScreen(
            state = ChatUiState(
                messages = listOf(
                    ChatMessage(
                        id = "user-1",
                        text = "Hello Gemini",
                        isUser = true
                    ),
                    ChatMessage(
                        id = "assistant-1",
                        text = "Hello! How can I help?",
                        isUser = false
                    )
                )
            )
        )

        composeRule
            .onNodeWithText("Hello Gemini")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Hello! How can I help?")
            .assertIsDisplayed()
    }

    @Test
    fun loadingShowsIndicatorAndDisablesInputActions() {
        showScreen(
            state = ChatUiState(
                prompt = "Another question",
                messages = listOf(
                    ChatMessage(
                        id = "user-1",
                        text = "Hello",
                        isUser = true
                    )
                ),
                isLoading = true
            )
        )

        composeRule
            .onNodeWithText("Gemini is thinking…")
            .assertIsDisplayed()

        composeRule
            .onNode(
                hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
            )
            .assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Send message")
            .assertIsNotEnabled()

        composeRule
            .onNodeWithContentDescription("Voice input")
            .assertIsNotEnabled()

        composeRule
            .onNodeWithTag("message_input")
            .assertIsNotEnabled()
    }

    @Test
    fun selectingDarkThemeInvokesCallback() {
        var selectedTheme: ThemePreference? = null

        showScreen(
            onThemeChange = { theme ->
                selectedTheme = theme
            }
        )

        composeRule
            .onNodeWithText("Dark")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(ThemePreference.DARK, selectedTheme)
        }
    }

    @Test
    fun microphoneButtonInvokesVoiceCallback() {
        var voiceRequests = 0

        showScreen(
            onVoiceInput = {
                voiceRequests++
            }
        )

        composeRule
            .onNodeWithContentDescription("Voice input")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, voiceRequests)
        }
    }

    @Test
    fun errorMessageAppearsInSnackbar() {
        showScreen(
            state = ChatUiState(
                errorMessage = "Test connection error"
            )
        )

        composeRule
            .onNodeWithText("Test connection error")
            .assertIsDisplayed()
    }
}
