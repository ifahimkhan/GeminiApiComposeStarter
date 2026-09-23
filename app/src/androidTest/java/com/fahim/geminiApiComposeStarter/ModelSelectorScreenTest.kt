package com.fahim.geminiApiComposeStarter

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fahim.geminiApiComposeStarter.data.AiProvider
import com.fahim.geminiApiComposeStarter.ui.model.ModelSelectorScreen
import com.fahim.geminiApiComposeStarter.ui.theme.N088Assignment1Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelSelectorScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun pickerShowsProvidersAndSelectsOpenAI() {
        var selected: AiProvider? = null
        composeRule.setContent {
            N088Assignment1Theme {
                ModelSelectorScreen(
                    selectedProvider = AiProvider.GEMINI,
                    availableProviders = setOf(AiProvider.GEMINI),
                    widthSizeClass = WindowWidthSizeClass.Compact,
                    onSelect = { selected = it },
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Gemini").assertExists()
        composeRule.onNodeWithText("DeepSeek").assertExists()
        composeRule.onNodeWithText("Add OPENAI_API_KEY to local.properties").assertExists()
        composeRule.onNodeWithText("OpenAI").performClick()
        assertEquals(AiProvider.OPENAI, selected)
    }
}
