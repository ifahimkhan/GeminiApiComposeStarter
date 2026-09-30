package com.fahim.geminiApiComposeStarter.ui.chat.components

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

/**
 * Helper utility for launching Speech-to-Text voice recognition.
 * Configures the RecognizerIntent with free-form speech model and prompt.
 */
object VoiceInputHelper {

    fun createSpeechIntent(context: Context, promptText: String = "Speak to Gemini..."): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, promptText)
        }
    }
}
