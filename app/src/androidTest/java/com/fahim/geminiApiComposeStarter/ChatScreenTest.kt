package com.fahim.geminiApiComposeStarter.ui.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_showsRadioCheckText() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = {},
                    onErrorDismissed = {},
                    onChipSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("RADIO CHECK. ASK ME ANYTHING.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_showsSuggestionChips() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = {},
                    onErrorDismissed = {},
                    onChipSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("EXPLAIN DRS", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("TYRE STRATEGY BASICS", substring = true).assertIsDisplayed()
    }

    @Test
    fun loading_showsTyreLoaderWithContentDescription() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                id = 1L,
                                sender = "PITWALL",
                                text = "",
                                timestamp = "14:32",
                                isLoading = true
                            )
                        ),
                        isLoading = true
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = {},
                    onErrorDismissed = {},
                    onChipSelected = {}
                )
            }
        }
        composeTestRule.onNode(hasContentDescription("Loading")).assertIsDisplayed()
    }

    @Test
    fun conversation_showsUserAndBotBubbles() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                id = 1L, sender = "DRIVER",
                                text = "Explain DRS", timestamp = "14:31"
                            ),
                            ChatMessage(
                                id = 2L, sender = "PITWALL",
                                text = "DRS reduces aerodynamic drag.", timestamp = "14:32"
                            )
                        )
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = {},
                    onErrorDismissed = {},
                    onChipSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Explain DRS").assertIsDisplayed()
        composeTestRule.onNodeWithText("DRS reduces aerodynamic drag.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun suggestionChip_click_triggersOnChipSelected() {
        var selectedChip = ""
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = {},
                    onErrorDismissed = {},
                    onChipSelected = { selectedChip = it }
                )
            }
        }
        composeTestRule.onNodeWithText("EXPLAIN DRS", substring = true).performClick()
        assert(selectedChip == "Explain DRS")
    }

    @Test
    fun pitwallTopBar_showsTitlePITWALL() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(),
                    onPromptChange = {},
                    onSend = {},
                    onClearChat = {},
                    onErrorDismissed = {},
                    onChipSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("PITWALL").assertIsDisplayed()
    }
}
