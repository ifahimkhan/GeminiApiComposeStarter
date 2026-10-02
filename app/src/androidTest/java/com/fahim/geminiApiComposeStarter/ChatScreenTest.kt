package com.fahim.geminiApiComposeStarter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.local.ChatMessageEntity
import com.fahim.geminiApiComposeStarter.ui.chat.ChatScreen
import com.fahim.geminiApiComposeStarter.ui.chat.ChatUiState
import com.fahim.geminiApiComposeStarter.ui.theme.GeminiApiComposeStarterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messagesRenderCorrectlyForUserAndAssistant() {
        val testMessages = listOf(
            ChatMessageEntity(id = 1, text = "What is Jetpack Compose?", isUser = true),
            ChatMessageEntity(
                id = 2,
                text = "Compose is Android's modern toolkit for building native UI.",
                isUser = false
            )
        )

        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = testMessages,
                        userName = "Ranish"
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearChat = {},
                    onDeleteMessage = {},
                    onToggleSettings = {},
                    onUpdateUserName = {},
                    onUpdateSystemPrompt = {},
                    onDismissError = {}
                )
            }
        }

        // Verify the user message is displayed
        composeTestRule
            .onNodeWithText("What is Jetpack Compose?")
            .assertIsDisplayed()

        // Verify the assistant message is displayed
        composeTestRule
            .onNodeWithText("Compose is Android's modern toolkit for building native UI.")
            .assertIsDisplayed()

        // Verify the user label and Gemini assistant label are displayed
        composeTestRule
            .onNodeWithText("Ranish")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Gemini")
            .assertIsDisplayed()
    }

    @Test
    fun loadingStateDisplaysProgressIndicators() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = listOf(ChatMessageEntity(id = 1, text = "Test prompt", isUser = true)),
                        isLoading = true
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearChat = {},
                    onDeleteMessage = {},
                    onToggleSettings = {},
                    onUpdateUserName = {},
                    onUpdateSystemPrompt = {},
                    onDismissError = {}
                )
            }
        }

        // Verify loading indicators and status text appear
        composeTestRule
            .onNodeWithText("Gemini is thinking...")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Crafting response...")
            .assertIsDisplayed()
    }

    @Test
    fun emptyStateDisplaysWelcomeGreetingAndSuggestions() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        messages = emptyList(),
                        isLoading = false,
                        userName = "Alice"
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearChat = {},
                    onDeleteMessage = {},
                    onToggleSettings = {},
                    onUpdateUserName = {},
                    onUpdateSystemPrompt = {},
                    onDismissError = {}
                )
            }
        }

        // Verify personalized greeting appears
        composeTestRule
            .onNodeWithText("Hello, Alice!")
            .assertIsDisplayed()

        // Verify suggested prompt section is displayed
        composeTestRule
            .onNodeWithText("Suggested Prompts")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Explain quantum computing in simple terms")
            .assertIsDisplayed()
    }

    @Test
    fun inputBarContainsVoiceButtonAndSendAction() {
        composeTestRule.setContent {
            GeminiApiComposeStarterTheme {
                ChatScreen(
                    state = ChatUiState(
                        prompt = "How does Room work?",
                        isLoading = false
                    ),
                    onPromptChange = {},
                    onSend = {},
                    onVoiceInput = {},
                    onClearChat = {},
                    onDeleteMessage = {},
                    onToggleSettings = {},
                    onUpdateUserName = {},
                    onUpdateSystemPrompt = {},
                    onDismissError = {}
                )
            }
        }

        // Verify voice input microphone button is present
        composeTestRule
            .onNodeWithContentDescription("Voice Input")
            .assertIsDisplayed()

        // Verify send icon button is present
        composeTestRule
            .onNodeWithContentDescription("Send")
            .assertIsDisplayed()
    }
}
