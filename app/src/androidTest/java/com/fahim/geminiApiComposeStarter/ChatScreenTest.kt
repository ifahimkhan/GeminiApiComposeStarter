package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.fahim.geminiApiComposeStarter.data.ChatMessage
import com.fahim.geminiApiComposeStarter.data.ThemeMode
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.chat.PromptError
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rendersDistinctConversationBubbles() {
        compose.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(ChatUiState(isInitializing = false, messages = listOf(
                    ChatMessage(1, "What is Compose?", true),
                    ChatMessage(2, "A modern Android UI toolkit.", false),
                )), {}, {})
            }
        }
        compose.onNodeWithText("You").assertIsDisplayed()
        compose.onNodeWithText("Gemini").assertIsDisplayed()
        compose.onNodeWithText("A modern Android UI toolkit.").assertIsDisplayed()
    }

    @Test fun typedInputIsHoistedAndSendInvokesCallback() {
        var state by mutableStateOf(ChatUiState(isInitializing = false))
        var sent = ""
        compose.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(state, { state = state.copy(prompt = it) }, { sent = state.prompt })
            }
        }
        compose.onNodeWithTag("prompt").performTextInput("Hello Gemini")
        compose.onNodeWithText("Send").performClick()
        compose.runOnIdle { assertEquals("Hello Gemini", sent) }
    }

    @Test fun loadingDisablesSendAndVoiceAndShowsProgress() {
        compose.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(ChatUiState(isInitializing = false, isLoading = true), {}, {})
            }
        }
        compose.onNodeWithTag("response_progress").assertIsDisplayed()
        compose.onNodeWithText("Send").assertIsNotEnabled()
        compose.onNodeWithText("Voice").assertIsNotEnabled()
    }

    @Test fun emptyValidationAndSnackbarAreVisible() {
        compose.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(ChatUiState(isInitializing = false, promptError = PromptError.EMPTY,
                    errorMessage = "Connection unavailable"), {}, {})
            }
        }
        compose.onNodeWithText("Enter a message first").assertIsDisplayed()
        compose.onNodeWithText("Connection unavailable").assertIsDisplayed()
    }

    @Test fun voiceResultCanPopulatePromptAndThemeSelectionIsHoisted() {
        var state by mutableStateOf(ChatUiState(isInitializing = false))
        compose.setContent {
            GeminiApiComposeStarterTheme(darkTheme = state.theme == ThemeMode.DARK) {
                ChatScreen(state, {}, {}, onVoice = { state = state.copy(prompt = "Recognized words") },
                    onThemeChange = { state = state.copy(theme = it) })
            }
        }
        compose.onNodeWithText("Voice").performClick()
        compose.onNodeWithTag("prompt").assertTextContains("Recognized words")
        compose.onNodeWithText("Dark").performClick()
        compose.runOnIdle { assertEquals(ThemeMode.DARK, state.theme) }
    }

    @Test fun clearRequiresConfirmation() {
        var confirmed = false
        compose.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(ChatUiState(isInitializing = false, showClearConfirmation = true), {}, {},
                    onClearConfirm = { confirmed = true })
            }
        }
        compose.onNodeWithText("Clear conversation?").assertIsDisplayed()
        compose.onNodeWithText("Clear messages").performClick()
        compose.runOnIdle { assertTrue(confirmed) }
    }

    @Test fun expandedLayoutKeepsInputAndActionsUsable() {
        compose.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(ChatUiState(isInitializing = false), {}, {}, widthClass = WindowWidthSizeClass.Expanded)
            }
        }
        compose.onNodeWithTag("prompt").assertIsDisplayed()
        compose.onNodeWithText("Send").assertIsDisplayed()
        compose.onNodeWithText("Start a conversation").assertIsDisplayed()
    }
}
