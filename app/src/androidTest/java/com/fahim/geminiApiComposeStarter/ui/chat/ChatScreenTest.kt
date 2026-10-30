package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dummyWindowSizeClass = WindowSizeClass.calculateFromSize(
        size = DpSize(360.dp, 800.dp),
    )

    @Test
    fun messages_areDisplayed() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage(id = "1", text = "Hello Gemini!", isUser = true),
                ChatMessage(id = "2", text = "Hello! How can I help you today?", isUser = false),
            ),
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    windowSizeClass = dummyWindowSizeClass,
                    onPromptChange = {},
                    onSend = {},
                    onDismissError = {},
                    onRetry = {},
                    onShowSpeechError = {},
                    onShowClearHistoryDialog = {},
                    onConfirmClearHistory = {},
                    onShowSettingsDialog = {},
                    onSetThemeMode = {},
                    onSetDynamicColor = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Hello Gemini!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello! How can I help you today?").assertIsDisplayed()
    }

    @Test
    fun loadingIndicator_appearsWhenIsLoadingIsTrue() {
        val state = ChatUiState(
            messages = listOf(
                ChatMessage(id = "1", text = "Tell me a joke.", isUser = true),
            ),
            isLoading = true,
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    windowSizeClass = dummyWindowSizeClass,
                    onPromptChange = {},
                    onSend = {},
                    onDismissError = {},
                    onRetry = {},
                    onShowSpeechError = {},
                    onShowClearHistoryDialog = {},
                    onConfirmClearHistory = {},
                    onShowSettingsDialog = {},
                    onSetThemeMode = {},
                    onSetDynamicColor = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Gemini is thinking...").assertIsDisplayed()
    }

    @Test
    fun sendButton_isDisabledWhileLoading() {
        val state = ChatUiState(
            prompt = "Some prompt",
            isLoading = true,
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    windowSizeClass = dummyWindowSizeClass,
                    onPromptChange = {},
                    onSend = {},
                    onDismissError = {},
                    onRetry = {},
                    onShowSpeechError = {},
                    onShowClearHistoryDialog = {},
                    onConfirmClearHistory = {},
                    onShowSettingsDialog = {},
                    onSetThemeMode = {},
                    onSetDynamicColor = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("send", substring = true, ignoreCase = true)
            .assertIsNotEnabled()
    }

    @Test
    fun errorState_isShown() {
        val state = ChatUiState(
            errorMessage = "Network connection failed",
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    windowSizeClass = dummyWindowSizeClass,
                    onPromptChange = {},
                    onSend = {},
                    onDismissError = {},
                    onRetry = {},
                    onShowSpeechError = {},
                    onShowClearHistoryDialog = {},
                    onConfirmClearHistory = {},
                    onShowSettingsDialog = {},
                    onSetThemeMode = {},
                    onSetDynamicColor = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Network connection failed").assertIsDisplayed()
    }

    @Test
    fun typingTextThenTappingSend_callsCallback() {
        var sendClicked = false

        val state = ChatUiState(
            prompt = "What is Kotlin?",
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = state,
                    windowSizeClass = dummyWindowSizeClass,
                    onPromptChange = {},
                    onSend = { sendClicked = true },
                    onDismissError = {},
                    onRetry = {},
                    onShowSpeechError = {},
                    onShowClearHistoryDialog = {},
                    onConfirmClearHistory = {},
                    onShowSettingsDialog = {},
                    onSetThemeMode = {},
                    onSetDynamicColor = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("send", substring = true, ignoreCase = true)
            .performClick()

        assertTrue(sendClicked)
    }
}
